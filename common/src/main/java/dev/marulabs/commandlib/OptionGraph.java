package dev.marulabs.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The option nodes of one command, as a graph of states. A state counts how often each option was given; the tokens
 * available in a state lead to the state with the counts of their options increased. Every option node built for the
 * command has a requirement that names this graph ({@link OptionNode}), so the runner counts the command's own option
 * tokens and not those of a parent command on the same path.
 * <p>
 * With redirects, the first token that reaches a state (in the order Brigadier and the vanilla command packet walk the
 * tree) holds the nodes after that state, and every other token that reaches it redirects there. The tree then has a
 * node per token of each state instead of one per order in which the options can be given. A redirect starts a new
 * Brigadier context, so the context of an execution after options has only the nodes after the last option.
 * {@link #recover} parses the options before it again from the input.
 * <p>
 * The re-parse needs a command whose path is literals only, as an argument child's inherited arguments would be lost
 * too, and option values that parse the same without a server. Other commands keep a tree without redirects, in which
 * the paths with the same counts share their child nodes.
 */
final class OptionGraph<S, C extends CommonCommandContext<S, ?>> {
    private final List<CommandOption<?, C>> options;
    private final BiPredicate<CommandOption<?, C>, CommandOption<?, C>> excludes;
    private final boolean redirects;
    // The names (and aliases) of the literals on the path to the command, from the root.
    private final List<Set<String>> path;
    private final int maxTokens;
    private final Map<Map<CommandOption<?, C>, Integer>, Map<String, Transition<S, C>>> transitions = new HashMap<>();

    private OptionGraph(List<CommandOption<?, C>> options,
                        BiPredicate<CommandOption<?, C>, CommandOption<?, C>> excludes,
                        boolean redirects,
                        List<Set<String>> path) {
        this.options = List.copyOf(options);
        this.excludes = excludes;
        this.redirects = redirects;
        this.path = List.copyOf(path);
        this.maxTokens = options.stream()
                                .mapToInt(x -> (x.hasValue() ? 2 : 1) * x.maxOccurrences())
                                .sum();
    }

    /**
     * The graph of the command's options. {@code inheritedArguments} are the arguments parsed before the command, which
     * an argument child has.
     */
    static <S, C extends CommonCommandContext<S, ?>> OptionGraph<S, C> of(CommonCommand<C, ?> command,
                                                                          List<Arguments<C>> inheritedArguments) {
        List<CommandOption<?, C>> options = command.options();
        boolean redirects = inheritedArguments.isEmpty() && options.stream()
                                                                   .filter(CommandOption::hasValue)
                                                                   .allMatch(x -> parsesWithoutServer(x.type()));
        Deque<Set<String>> path = new ArrayDeque<>();
        for (CommonCommand<C, ?> current = command; current != null; current = current.parent()) {
            Set<String> names = new LinkedHashSet<>();
            names.add(current.name());
            names.addAll(current.aliases());
            path.addFirst(names);
        }
        return new OptionGraph<>(options, command::excludes, redirects, new ArrayList<>(path));
    }

    private static boolean parsesWithoutServer(ArgumentType<?> type) {
        if (type instanceof StringArgumentType) {
            return ((StringArgumentType) type).getType() != StringArgumentType.StringType.GREEDY_PHRASE;
        }
        return type instanceof BoolArgumentType || type instanceof IntegerArgumentType
                || type instanceof LongArgumentType || type instanceof FloatArgumentType
                || type instanceof DoubleArgumentType;
    }

    List<CommandOption<?, C>> options() {
        return options;
    }

    boolean usesRedirects() {
        return redirects;
    }

    /**
     * Builds the option nodes and returns the children of the command's own node: the argument nodes and the option
     * tokens of the empty state. The argument nodes follow every state, as the same node objects.
     */
    List<CommandNode<S>> build(List<CommandNode<S>> argumentNodes, Command<S> executor) {
        if (options.isEmpty()) {
            return argumentNodes;
        }
        return new Builder(argumentNodes, executor).children(Map.of());
    }

    private final class Builder {
        private final List<CommandNode<S>> argumentNodes;
        private final Command<S> executor;
        private final Map<Map<CommandOption<?, C>, Integer>, List<CommandNode<S>>> childrenByState = new HashMap<>();
        // The node that holds the children of each state, which the other tokens reaching the state redirect to.
        private final Map<Map<CommandOption<?, C>, Integer>, CommandNode<S>> hubs = new HashMap<>();

        private Builder(List<CommandNode<S>> argumentNodes, Command<S> executor) {
            this.argumentNodes = argumentNodes;
            this.executor = executor;
        }

        // Builds depth first, children in order, as the vanilla command packet copies the tree for a player. A hub is
        // then copied before every redirect to it, which the copy resolves to the copied hub.
        private List<CommandNode<S>> children(Map<CommandOption<?, C>, Integer> used) {
            List<CommandNode<S>> cached = childrenByState.get(used);
            if (cached != null) {
                return cached;
            }
            List<CommandNode<S>> nodes = new ArrayList<>(argumentNodes);
            Map<String, Transition<S, C>> stateTransitions = new HashMap<>();
            for (OptionToken<C> token : tokens(used)) {
                Map<CommandOption<?, C>, Integer> next = next(used, token.options);
                if (token.isValue()) {
                    CommandOption<?, C> option = token.options.get(0);
                    RequiredArgumentBuilder<S, ?> value = RequiredArgumentBuilder.<S, Object>argument(option.internalName(used.getOrDefault(option,
                                                                                                                                            0)),
                                                                                                      cast(option.type()))
                                                                                 .requires(new OptionNode<>(OptionGraph.this,
                                                                                                            List.of(),
                                                                                                            option,
                                                                                                            next))
                                                                                 .executes(executor);
                    CommandNode<S> valueNode = target(value, next);
                    LiteralCommandNode<S> literal = LiteralArgumentBuilder.<S>literal(token.token)
                                                                          .requires(new OptionNode<>(OptionGraph.this,
                                                                                                     List.of(),
                                                                                                     null,
                                                                                                     null))
                                                                          .build();
                    literal.addChild(valueNode);
                    nodes.add(literal);
                    stateTransitions.put(token.token, new Transition<>(option, next, literal, valueNode));
                } else {
                    LiteralArgumentBuilder<S> literal = LiteralArgumentBuilder.<S>literal(token.token)
                                                                              .requires(new OptionNode<>(OptionGraph.this,
                                                                                                         token.options,
                                                                                                         null,
                                                                                                         next))
                                                                              .executes(executor);
                    CommandNode<S> node = target(literal, next);
                    nodes.add(node);
                    stateTransitions.put(token.token, new Transition<>(null, next, node, null));
                }
            }
            childrenByState.put(used, nodes);
            transitions.put(used, stateTransitions);
            return nodes;
        }

        // The node for a token that leads to the given state: a redirect to the state's hub, or else the hub itself.
        private CommandNode<S> target(ArgumentBuilder<S, ?> builder, Map<CommandOption<?, C>, Integer> state) {
            CommandNode<S> hub = redirects ? hubs.get(state) : null;
            if (hub != null) {
                return builder.redirect(hub)
                              .build();
            }
            CommandNode<S> node = builder.build();
            if (redirects) {
                hubs.put(state, node);
            }
            children(state).forEach(node::addChild);
            return node;
        }
    }

    @SuppressWarnings("unchecked")
    private static ArgumentType<Object> cast(ArgumentType<?> type) {
        return (ArgumentType<Object>) type;
    }

    private static <C extends CommonCommandContext<?, ?>> Map<CommandOption<?, C>, Integer> next(Map<CommandOption<?, C>, Integer> used,
                                                                                                 List<CommandOption<?, C>> options) {
        Map<CommandOption<?, C>, Integer> next = new HashMap<>(used);
        options.forEach(x -> next.merge(x, 1, Integer::sum));
        return Map.copyOf(next);
    }

    // The tokens of a state. An option is available while it was given fewer times than its maximum and no option of
    // one of its mutually exclusive groups was given.
    private List<OptionToken<C>> tokens(Map<CommandOption<?, C>, Integer> used) {
        List<OptionToken<C>> tokens = new ArrayList<>();
        List<CommandOption<?, C>> remaining = options.stream()
                                                     .filter(x -> used.getOrDefault(x, 0) < x.maxOccurrences())
                                                     .filter(x -> used.keySet()
                                                                      .stream()
                                                                      .noneMatch(y -> excludes.test(x, y)))
                                                     .collect(Collectors.toList());

        for (CommandOption<?, C> option : remaining) {
            for (String name : option.names()) {
                tokens.add(new OptionToken<>("--" + name, List.of(option)));
            }
            Character shortName = option.shortName();
            if (shortName == null) {
                continue;
            }
            tokens.add(new OptionToken<>("-" + shortName, List.of(option)));
            if (option.hasValue()) {
                continue;
            }
            // A counted flag can repeat its short name in one token, such as -vvv.
            int left = option.maxOccurrences() - used.getOrDefault(option, 0);
            for (int count = 2; count <= left; count++) {
                tokens.add(new OptionToken<>("-" + String.valueOf(shortName)
                                                         .repeat(count),
                                             Collections.nCopies(count, option)));
            }
        }

        // Every order of two or more flags with short names, such as -fv and -vf.
        List<CommandOption<?, C>> shortFlags = remaining.stream()
                                                        .filter(x -> x.shortName() != null && !x.hasValue())
                                                        .collect(Collectors.toList());
        for (int size = 2; size <= shortFlags.size(); size++) {
            permutations(shortFlags, size, new ArrayList<>(), tokens);
        }
        return tokens;
    }

    private void permutations(List<CommandOption<?, C>> flags,
                              int size,
                              List<CommandOption<?, C>> current,
                              List<OptionToken<C>> dest) {
        if (current.size() == size) {
            dest.add(new OptionToken<>("-" + current.stream()
                                                    .map(x -> String.valueOf(x.shortName()))
                                                    .collect(Collectors.joining()),
                                       List.copyOf(current)));
            return;
        }
        for (CommandOption<?, C> flag : flags) {
            if (current.contains(flag) || current.stream()
                                                 .anyMatch(x -> excludes.test(x, flag))) {
                continue;
            }
            current.add(flag);
            permutations(flags, size, current, dest);
            current.remove(current.size() - 1);
        }
    }

    /**
     * The context of an execution as one context with the options given before it. Brigadier executes the context
     * after the last redirect, which for input that gave options holds only the nodes after the last option; its root
     * node is then the hub of the state the options reached. The options are parsed again from the input, which ends
     * them before that context, and the literals of the command's path before them are added as they were typed.
     */
    @SuppressWarnings("unchecked")
    CommandContext<S> recover(CommandContext<S> context) {
        if (!redirects) {
            return context;
        }
        Predicate<S> requirement = context.getRootNode()
                                          .getRequirement();
        if (!(requirement instanceof OptionNode) || ((OptionNode<S>) requirement).graph() != this) {
            return context;
        }
        Map<? extends CommandOption<?, ?>, Integer> hubState = ((OptionNode<S>) requirement).state();
        if (hubState == null) {
            return context;
        }
        Map<CommandOption<?, C>, Integer> state = (Map<CommandOption<?, C>, Integer>) hubState;
        String input = context.getInput();
        int end = context.getRange()
                         .getStart()
                - 1;
        Region<S> region = end > 0 && input.charAt(end) == ' ' ? findRegion(input, end, state) : null;
        if (region == null) {
            throw new IllegalStateException("Cannot read the options before position " + (end + 1) + " of '" + input
                    + "'");
        }
        List<ParsedCommandNode<S>> nodes = new ArrayList<>(prefix(input, region.start));
        nodes.addAll(region.nodes);
        int start = nodes.get(0)
                         .getRange()
                         .getStart();
        CommandContext<S> options = new CommandContext<>(context.getSource(),
                                                         input,
                                                         region.arguments,
                                                         context.getCommand(),
                                                         context.getRootNode(),
                                                         nodes,
                                                         StringRange.between(start, end),
                                                         null,
                                                         null,
                                                         false);
        return ChainedCommandContext.of(List.of(options, context));
    }

    // The options ending at end (a space before the context) that reach the given state. Each option occurrence takes
    // one or two space-separated tokens, so the start is one of the last maxTokens token starts. A later start skips
    // occurrences, so it rarely reaches the same state, and the earliest start that does is taken: the token before
    // the true start is the command's literal, which is no option token.
    @Nullable
    private Region<S> findRegion(String input, int end, Map<CommandOption<?, C>, Integer> state) {
        List<Integer> starts = new ArrayList<>();
        int pos = end;
        while (starts.size() < maxTokens && pos > 0) {
            int space = input.lastIndexOf(' ', pos - 1);
            if (space + 1 < pos) {
                starts.add(space + 1);
            }
            pos = space;
        }
        for (int i = starts.size() - 1; i >= 0; i--) {
            Region<S> region = parse(input, starts.get(i), end);
            if (region != null && region.state.equals(state)) {
                return region;
            }
        }
        return null;
    }

    // Parses input[start, end) as option tokens from the empty state, as Brigadier matches them: a token is a literal
    // of the state up to the next space, and a value option's literal is followed by its value.
    @Nullable
    private Region<S> parse(String input, int start, int end) {
        Map<CommandOption<?, C>, Integer> state = Map.of();
        List<ParsedCommandNode<S>> nodes = new ArrayList<>();
        Map<String, ParsedArgument<S, ?>> arguments = new LinkedHashMap<>();
        int pos = start;
        while (true) {
            int tokenEnd = input.indexOf(' ', pos);
            if (tokenEnd < 0 || tokenEnd > end) {
                tokenEnd = end;
            }
            Map<String, Transition<S, C>> stateTransitions = transitions.get(state);
            Transition<S, C> transition = stateTransitions == null ? null
                    : stateTransitions.get(input.substring(pos, tokenEnd));
            if (transition == null) {
                return null;
            }
            nodes.add(new ParsedCommandNode<>(transition.literal, StringRange.between(pos, tokenEnd)));
            pos = tokenEnd;
            CommandOption<?, C> option = transition.valueOption;
            CommandNode<S> valueNode = transition.valueNode;
            if (option != null && valueNode != null) {
                if (pos >= end) {
                    return null;
                }
                pos++;
                StringReader reader = new StringReader(input);
                reader.setCursor(pos);
                Object value;
                try {
                    value = option.type()
                                  .parse(reader);
                } catch (CommandSyntaxException | RuntimeException e) {
                    return null;
                }
                int valueEnd = reader.getCursor();
                if (valueEnd <= pos || valueEnd > end) {
                    return null;
                }
                arguments.put(valueNode.getName(), new ParsedArgument<>(pos, valueEnd, value));
                nodes.add(new ParsedCommandNode<>(valueNode, StringRange.between(pos, valueEnd)));
                pos = valueEnd;
            }
            state = transition.next;
            if (pos == end) {
                return new Region<>(start, state, nodes, arguments);
            }
            if (input.charAt(pos) != ' ' || pos + 1 >= end) {
                return null;
            }
            pos++;
        }
    }

    // The literals of the command's path typed before the options, as a context without redirects has them. A
    // command label can have a namespace, such as plugin:name.
    private List<ParsedCommandNode<S>> prefix(String input, int start) {
        Deque<ParsedCommandNode<S>> nodes = new ArrayDeque<>();
        int pos = start - 1;
        for (int i = path.size() - 1; i >= 0 && pos > 0 && input.charAt(pos) == ' '; i--) {
            int tokenStart = input.lastIndexOf(' ', pos - 1) + 1;
            if (tokenStart == 0 && input.startsWith("/")) {
                tokenStart = 1;
            }
            String token = input.substring(tokenStart, pos);
            String name = i == 0 ? token.substring(token.lastIndexOf(':') + 1) : token;
            if (!path.get(i)
                     .contains(name)) {
                break;
            }
            nodes.addFirst(new ParsedCommandNode<>(LiteralArgumentBuilder.<S>literal(token)
                                                                         .build(),
                                                   StringRange.between(tokenStart, pos)));
            pos = tokenStart - 1;
        }
        return new ArrayList<>(nodes);
    }

    /**
     * The occurrences of each option in the parsed nodes, in input order: the internal argument name of each value,
     * and the token of each flag once per occurrence.
     */
    @SuppressWarnings("unchecked")
    Map<CommandOption<?, C>, List<String>> occurrences(CommandContext<S> handle) {
        Map<CommandOption<?, C>, List<String>> occurrences = new LinkedHashMap<>();
        for (ParsedCommandNode<S> parsed : handle.getNodes()) {
            Predicate<S> requirement = parsed.getNode()
                                             .getRequirement();
            if (!(requirement instanceof OptionNode) || ((OptionNode<S>) requirement).graph() != this) {
                continue;
            }
            OptionNode<S> node = (OptionNode<S>) requirement;
            String name = parsed.getNode()
                                .getName();
            for (CommandOption<?, ?> flag : node.flags()) {
                occurrences.computeIfAbsent((CommandOption<?, C>) flag, x -> new ArrayList<>())
                           .add(name);
            }
            CommandOption<?, ?> value = node.value();
            if (value != null) {
                occurrences.computeIfAbsent((CommandOption<?, C>) value, x -> new ArrayList<>())
                           .add(name);
            }
        }
        return occurrences;
    }

    private static final class OptionToken<C extends CommonCommandContext<?, ?>> {
        private final String token;
        private final List<CommandOption<?, C>> options;

        private OptionToken(String token, List<CommandOption<?, C>> options) {
            this.token = token;
            this.options = options;
        }

        private boolean isValue() {
            return options.size() == 1 && options.get(0)
                                                 .hasValue();
        }
    }

    private static final class Transition<S, C extends CommonCommandContext<S, ?>> {
        // The option whose value follows the literal, or null for flags.
        private final @Nullable CommandOption<?, C> valueOption;
        private final Map<CommandOption<?, C>, Integer> next;
        private final CommandNode<S> literal;
        private final @Nullable CommandNode<S> valueNode;

        private Transition(@Nullable CommandOption<?, C> valueOption,
                           Map<CommandOption<?, C>, Integer> next,
                           CommandNode<S> literal,
                           @Nullable CommandNode<S> valueNode) {
            this.valueOption = valueOption;
            this.next = next;
            this.literal = literal;
            this.valueNode = valueNode;
        }
    }

    private static final class Region<S> {
        private final int start;
        private final Map<?, Integer> state;
        private final List<ParsedCommandNode<S>> nodes;
        private final Map<String, ParsedArgument<S, ?>> arguments;

        private Region(int start,
                       Map<?, Integer> state,
                       List<ParsedCommandNode<S>> nodes,
                       Map<String, ParsedArgument<S, ?>> arguments) {
            this.start = start;
            this.state = state;
            this.nodes = nodes;
            this.arguments = arguments;
        }
    }
}
