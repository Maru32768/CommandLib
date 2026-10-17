package dev.marulabs.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.marulabs.commandlib.command.Extractor;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The option nodes of one command, as a graph of states. A state counts how often each option was given; the tokens
 * available in a state lead to the state with the counts of their options increased. Every option node built for the
 * command has a requirement that names this graph ({@link OptionNode}), so the runner counts the command's own option
 * tokens and not those of a parent command on the same path.
 * <p>
 * A command whose options may follow its arguments ({@link CommonCommand#optionsAnywhere()}) also keys a state by its
 * position, the argument node it follows or none before the first argument. Each argument node is then copied for
 * each count it follows, and offers the option tokens of that count; the tokens keep the position, and the arguments
 * advance it.
 * <p>
 * With redirects, the first token that reaches a state (in the order Brigadier and the vanilla command packet walk the
 * tree) holds the nodes after that state, and every other token that reaches it redirects there. The tree then has a
 * node per token of each state instead of one per order in which the options can be given. A redirect starts a new
 * Brigadier context, so the context of an execution after options has only the nodes after the last option.
 * {@link #recover} parses the options and arguments before it again from the input.
 * <p>
 * The re-parse needs a command whose path is literals only, as an argument child's inherited arguments would be lost
 * too. An argument child keeps a tree without redirects, in which the paths with the same state share their child
 * nodes. The options of the command it follows can still redirect into its path, so its runner recovers the input
 * with that command's graph.
 */
final class OptionGraph<S, C extends CommonCommandContext<S, ?>> {
    // Seven short flags make about 37,500 nodes and eight about 300,000, which take seconds to build and are too large
    // to send to players.
    private static final int MAX_NODES = 50_000;
    private final List<CommandOption<?, C>> options;
    private final BiPredicate<CommandOption<?, C>, CommandOption<?, C>> excludes;
    private final boolean redirects;
    private final boolean anywhere;
    // The names (and aliases) of the literals on the path to the command, from the root.
    private final List<Set<String>> path;
    // The suggestions of the value of an option of an argument.
    private final Function<CommonArgument<?, C, ?>, SuggestionProvider<S>> suggestions;
    // The permission node of each option that needs one.
    private final Map<CommandOption<?, C>, String> permissions;
    private final BiPredicate<S, String> hasPermission;
    private final Map<State, Map<String, Transition>> transitions = new HashMap<>();
    // The argument nodes the command's own node has, which the positions of the states refer to.
    private List<CommandNode<S>> argumentNodes = List.of();

    private OptionGraph(List<CommandOption<?, C>> options,
                        BiPredicate<CommandOption<?, C>, CommandOption<?, C>> excludes,
                        boolean redirects,
                        boolean anywhere,
                        List<Set<String>> path,
                        Function<CommonArgument<?, C, ?>, SuggestionProvider<S>> suggestions,
                        Map<CommandOption<?, C>, String> permissions,
                        BiPredicate<S, String> hasPermission) {
        this.options = List.copyOf(options);
        this.excludes = excludes;
        this.redirects = redirects;
        this.anywhere = anywhere;
        this.path = List.copyOf(path);
        this.suggestions = suggestions;
        this.permissions = Map.copyOf(permissions);
        this.hasPermission = hasPermission;
    }

    /**
     * The graph of the command's options. {@code inheritedArguments} are the arguments parsed before the command, which
     * an argument child has.
     */
    static <S, C extends CommonCommandContext<S, ?>> OptionGraph<S, C> of(PlatformAdapter<S, ?, C> platformAdapter,
                                                                          CommonCommand<C, ?> command,
                                                                          List<Arguments<C>> inheritedArguments,
                                                                          String permissionPrefix) {
        List<CommandOption<?, C>> options = command.options();
        // Aliases can be added after an option is registered, which option(...) could not check.
        CommandOption.checkDistinctNames(options, command.name());
        for (CommandOption<?, C> option : options) {
            for (CommandOption<?, ?> required : option.requiredOptions()) {
                if (!options.contains(required)) {
                    throw new IllegalStateException("Option " + option.formatName() + " of command '" + command.name()
                            + "' requires " + required.formatName() + ", which is not an option of the command");
                }
            }
        }
        Map<CommandOption<?, C>, String> permissions = new HashMap<>();
        for (CommandOption<?, C> option : options) {
            String permission = option.permissionName(command, permissionPrefix);
            if (permission != null) {
                permissions.put(option, permission);
            }
        }
        boolean redirects = inheritedArguments.isEmpty();
        Deque<Set<String>> path = new ArrayDeque<>();
        for (CommonCommand<C, ?> current = command; current != null; current = current.parent()) {
            Set<String> names = new LinkedHashSet<>();
            names.add(current.name());
            names.addAll(current.aliases());
            path.addFirst(names);
        }
        Map<ContextKey<?>, ? extends Extractor<?, ?>> providers = command.providers();
        return new OptionGraph<>(options,
                                 command::excludes,
                                 redirects,
                                 command.allowsOptionsAnywhere(),
                                 new ArrayList<>(path),
                                 argument -> ArgumentCommandNodeCreator.suggestions(platformAdapter,
                                                                                    argument,
                                                                                    source -> true,
                                                                                    providers,
                                                                                    inheritedArguments),
                                 permissions,
                                 platformAdapter::hasPermission);
    }

    List<CommandOption<?, C>> options() {
        return options;
    }

    /**
     * The permission node of the option, or null when it needs none.
     */
    @Nullable
    String permissionName(CommandOption<?, C> option) {
        return permissions.get(option);
    }

    // The requirement of a token of the given options: the sender has the permission of each.
    private Predicate<S> permitted(List<CommandOption<?, C>> tokenOptions) {
        List<String> nodes = tokenOptions.stream()
                                         .map(permissions::get)
                                         .filter(Objects::nonNull)
                                         .distinct()
                                         .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return source -> true;
        }
        return source -> nodes.stream()
                              .allMatch(x -> hasPermission.test(source, x));
    }

    /**
     * Builds the option nodes and returns the children of the command's own node: the argument nodes and the option
     * tokens of the empty state. The argument nodes follow every state, as the same node objects, unless the options
     * may follow the arguments; each argument node is then copied for each count of options it follows.
     */
    List<CommandNode<S>> build(List<CommandNode<S>> argumentNodes, Command<S> executor) {
        this.argumentNodes = List.copyOf(argumentNodes);
        if (options.isEmpty()) {
            return argumentNodes;
        }
        return new Builder(executor).children(new State(Map.of(), null));
    }

    private final class Builder {
        private final Command<S> executor;
        private final Map<State, List<CommandNode<S>>> childrenByState = new HashMap<>();
        // The node that holds the children of each state, which the other tokens reaching the state redirect to.
        private final Map<State, CommandNode<S>> hubs = new HashMap<>();
        // The copy of an argument node for the counts it follows, keyed by the state after it.
        private final Map<State, CommandNode<S>> copies = new HashMap<>();
        // The option tokens of the states built so far, about the number of nodes.
        private int tokenCount;

        private Builder(Command<S> executor) {
            this.executor = executor;
        }

        // Builds depth first, children in order, as the vanilla command packet copies the tree for a player. A hub is
        // then copied before every redirect to it, which the copy resolves to the copied hub.
        private List<CommandNode<S>> children(State state) {
            List<CommandNode<S>> cached = childrenByState.get(state);
            if (cached != null) {
                return cached;
            }
            List<CommandNode<S>> nodes = new ArrayList<>();
            if (anywhere) {
                for (CommandNode<S> argument : argumentsAfter(state.position)) {
                    nodes.add(copy(argument, state.counts));
                }
            } else {
                nodes.addAll(argumentNodes);
            }
            CommandNode<S> position = state.position;
            // Input that ends after the options runs what input ending at the position runs.
            Command<S> command = position == null ? executor : position.getCommand();
            Map<String, Transition> stateTransitions = new HashMap<>();
            List<OptionToken<C>> tokens = tokens(state.counts, MAX_NODES - tokenCount);
            tokenCount += tokens.size();
            if (tokenCount > MAX_NODES) {
                throw new IllegalStateException("The options of '" + String.join(" ",
                                                                                 path.stream()
                                                                                     .map(x -> x.iterator()
                                                                                                .next())
                                                                                     .collect(Collectors.toList()))
                        + "' make more than " + MAX_NODES + " command nodes. Give fewer options a short name, or lower "
                        + "their maximums; see docs/options.md#limits.");
            }
            for (OptionToken<C> token : tokens) {
                State next = new State(next(state.counts, token.options), position);
                if (token.isValue()) {
                    CommandOption<?, C> option = token.options.get(0);
                    RequiredArgumentBuilder<S, ?> value = RequiredArgumentBuilder.<S, Object>argument(option.internalName(state.counts.getOrDefault(option,
                                                                                                                                                    0)),
                                                                                                      cast(option.type()))
                                                                                 .requires(new OptionNode<>(OptionGraph.this,
                                                                                                            List.of(),
                                                                                                            option,
                                                                                                            next,
                                                                                                            permitted(List.of())))
                                                                                 .executes(command);
                    CommonArgument<?, C, ?> argument = option.suggestionArgument();
                    if (argument != null) {
                        value.suggests(suggestions.apply(argument));
                    }
                    CommandNode<S> valueNode = target(value, next);
                    LiteralCommandNode<S> literal = LiteralArgumentBuilder.<S>literal(token.token)
                                                                          .requires(new OptionNode<>(OptionGraph.this,
                                                                                                     List.of(),
                                                                                                     null,
                                                                                                     null,
                                                                                                     permitted(token.options)))
                                                                          .build();
                    literal.addChild(valueNode);
                    nodes.add(literal);
                    stateTransitions.put(token.token, new Transition(option, next, literal, valueNode));
                } else {
                    LiteralArgumentBuilder<S> literal = LiteralArgumentBuilder.<S>literal(token.token)
                                                                              .requires(new OptionNode<>(OptionGraph.this,
                                                                                                         token.options,
                                                                                                         null,
                                                                                                         next,
                                                                                                         permitted(token.options)))
                                                                              .executes(command);
                    CommandNode<S> node = target(literal, next);
                    nodes.add(node);
                    stateTransitions.put(token.token, new Transition(null, next, node, null));
                }
            }
            childrenByState.put(state, nodes);
            transitions.put(state, stateTransitions);
            return nodes;
        }

        // The node for a token that leads to the given state: a redirect to the state's hub, or else the hub itself.
        private CommandNode<S> target(ArgumentBuilder<S, ?> builder, State state) {
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

        // The argument node for the given counts: a copy that offers the option tokens after it, with the copies of
        // the argument nodes after it. A variadic argument's nodes and the child commands are shared. The copy is a
        // node of the state after it like a token: it redirects to the state's hub or is the hub itself, unless the
        // argument has child commands, which only the argument and no option token after it may lead to.
        private CommandNode<S> copy(CommandNode<S> argument, Map<CommandOption<?, C>, Integer> counts) {
            if (!(argument instanceof ArgumentCommandNode) || isVariadic(argument)) {
                return argument;
            }
            State state = new State(counts, argument);
            CommandNode<S> cached = copies.get(state);
            if (cached != null) {
                return cached;
            }
            List<CommandNode<S>> commands = argument.getChildren()
                                                    .stream()
                                                    .filter(x -> !(x instanceof ArgumentCommandNode))
                                                    .collect(Collectors.toList());
            // The requirement names the state, so the runner recovers the input before a redirect to the copy.
            ArgumentBuilder<S, ?> builder = argument.createBuilder()
                                                    .requires(new OptionNode<>(OptionGraph.this,
                                                                               List.of(),
                                                                               null,
                                                                               state,
                                                                               argument.getRequirement()));
            CommandNode<S> node;
            if (commands.isEmpty()) {
                node = target(builder, state);
            } else {
                node = builder.build();
                children(state).forEach(node::addChild);
                commands.forEach(node::addChild);
            }
            copies.put(state, node);
            return node;
        }
    }

    // The argument nodes that may follow the position: those of the command's node, or the argument children of the
    // argument node.
    private List<CommandNode<S>> argumentsAfter(@Nullable CommandNode<S> position) {
        if (position == null) {
            return argumentNodes;
        }
        return position.getChildren()
                       .stream()
                       .filter(x -> x instanceof ArgumentCommandNode)
                       .collect(Collectors.toList());
    }

    // A variadic argument repeats its node under one name. Options do not follow its values, so its nodes are shared.
    private static boolean isVariadic(CommandNode<?> argument) {
        return argument.getChildren()
                       .stream()
                       .anyMatch(x -> x instanceof ArgumentCommandNode && x.getName()
                                                                           .equals(argument.getName()));
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
    // one of its mutually exclusive groups was given. Generation stops once the tokens exceed the budget, as the orders
    // of many short flags grow factorially.
    private List<OptionToken<C>> tokens(Map<CommandOption<?, C>, Integer> used, int budget) {
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
            permutations(shortFlags, size, new ArrayList<>(), tokens, budget);
        }
        return tokens;
    }

    private void permutations(List<CommandOption<?, C>> flags,
                              int size,
                              List<CommandOption<?, C>> current,
                              List<OptionToken<C>> dest,
                              int budget) {
        if (dest.size() > budget) {
            return;
        }
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
            permutations(flags, size, current, dest, budget);
            current.remove(current.size() - 1);
        }
    }

    /**
     * The context of an execution as one context with the options given before it. Brigadier executes the context
     * after the last redirect, which for input that gave options holds only the nodes after the last option; its root
     * node is then the hub of the state the options reached. The options, and the arguments between them, are parsed
     * again from the input, which ends them before that context, and the literals of the command's path before them
     * are added as they were typed.
     */
    @SuppressWarnings("unchecked")
    CommandContext<S> recover(CommandContext<S> context) throws CommandSyntaxException {
        Predicate<S> requirement = context.getRootNode()
                                          .getRequirement();
        if (!(requirement instanceof OptionNode)) {
            return context;
        }
        OptionGraph<S, ?> graph = ((OptionNode<S>) requirement).graph();
        if (graph != this) {
            // An argument child has no redirects of its own, but the options of the command it follows can redirect
            // into its path, past arguments it inherits. That command's graph parses them again.
            return redirects ? context : graph.recover(context);
        }
        if (!redirects) {
            return context;
        }
        Object hubState = ((OptionNode<S>) requirement).state();
        if (!(hubState instanceof OptionGraph.State)) {
            return context;
        }
        String input = context.getInput();
        int end = context.getRange()
                         .getStart()
                - 1;
        if (end + 1 >= input.length()) {
            // Brigadier follows a redirect once any character follows the token, here a trailing space, and a word
            // argument then parses the empty rest. Without the redirect it rejected the space, as it still does.
            StringReader reader = new StringReader(input);
            reader.setCursor(Math.max(end, 0));
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()
                                                            .createWithContext(reader);
        }
        Region region = end > 0 && input.charAt(end) == ' ' ? findRegion(input, end, (State) hubState) : null;
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

    // The options (and arguments) ending at end, a space before the context, that reach the given state. They start
    // after the command's literal, the last of its path, so each token after that literal is a start; the earliest
    // start that parses to the state is taken, since an option value or argument can repeat the literal.
    @Nullable
    private Region findRegion(String input, int end, State state) {
        Set<String> names = path.get(path.size() - 1);
        for (int space = input.indexOf(' '); space >= 0 && space < end; space = input.indexOf(' ', space + 1)) {
            int tokenStart = input.lastIndexOf(' ', space - 1) + 1;
            String token = input.substring(tokenStart, space);
            if (tokenStart == 0 && token.startsWith("/")) {
                token = token.substring(1);
            }
            if (path.size() == 1) {
                // A command label can have a namespace, such as plugin:name.
                token = token.substring(token.lastIndexOf(':') + 1);
            }
            if (!names.contains(token)) {
                continue;
            }
            List<ParsedCommandNode<S>> nodes = new ArrayList<>();
            Map<String, ParsedArgument<S, ?>> arguments = new LinkedHashMap<>();
            if (step(input, space + 1, end, new State(Map.of(), null), state, nodes, arguments)) {
                return new Region(space + 1, nodes, arguments);
            }
        }
        return null;
    }

    // Parses input from pos as Brigadier matches it: a token is a literal of the state up to the next space, and a
    // value option's literal is followed by its value. A token that is no literal of the state is an argument that may
    // follow the position, parsed with the argument's type, when the options may follow the arguments.
    private boolean step(String input,
                         int pos,
                         int end,
                         State state,
                         State target,
                         List<ParsedCommandNode<S>> nodes,
                         Map<String, ParsedArgument<S, ?>> arguments) {
        int tokenEnd = input.indexOf(' ', pos);
        if (tokenEnd < 0 || tokenEnd > end) {
            tokenEnd = end;
        }
        Map<String, Transition> stateTransitions = transitions.get(state);
        Transition transition = stateTransitions == null ? null : stateTransitions.get(input.substring(pos, tokenEnd));
        if (transition != null) {
            // Brigadier takes only the literal that matches a token.
            int size = nodes.size();
            nodes.add(new ParsedCommandNode<>(transition.literal, StringRange.between(pos, tokenEnd)));
            int next = tokenEnd;
            CommandOption<?, C> option = transition.valueOption;
            CommandNode<S> valueNode = transition.valueNode;
            if (option != null && valueNode != null) {
                next = next < end ? parseValue(input, next + 1, end, valueNode, option.type(), nodes, arguments) : -1;
            }
            if (next >= 0 && advance(input, next, end, transition.next, target, nodes, arguments)) {
                return true;
            }
            nodes.subList(size, nodes.size())
                 .clear();
            if (valueNode != null) {
                arguments.remove(valueNode.getName());
            }
            return false;
        }
        if (!anywhere) {
            return false;
        }
        for (CommandNode<S> argument : argumentsAfter(state.position)) {
            if (isVariadic(argument)) {
                continue;
            }
            int size = nodes.size();
            int next = parseValue(input,
                                  pos,
                                  end,
                                  argument,
                                  ((ArgumentCommandNode<S, ?>) argument).getType(),
                                  nodes,
                                  arguments);
            if (next >= 0 && advance(input, next, end, new State(state.counts, argument), target, nodes, arguments)) {
                return true;
            }
            nodes.subList(size, nodes.size())
                 .clear();
            arguments.remove(argument.getName());
        }
        return false;
    }

    private boolean advance(String input,
                            int pos,
                            int end,
                            State state,
                            State target,
                            List<ParsedCommandNode<S>> nodes,
                            Map<String, ParsedArgument<S, ?>> arguments) {
        if (pos == end) {
            return state.equals(target);
        }
        if (input.charAt(pos) != ' ' || pos + 1 >= end) {
            return false;
        }
        return step(input, pos + 1, end, state, target, nodes, arguments);
    }

    // Parses the value of an argument node at pos and returns its end, or -1 when it does not parse up to end.
    private int parseValue(String input,
                           int pos,
                           int end,
                           CommandNode<S> node,
                           ArgumentType<?> type,
                           List<ParsedCommandNode<S>> nodes,
                           Map<String, ParsedArgument<S, ?>> arguments) {
        StringReader reader = new StringReader(input);
        reader.setCursor(pos);
        Object value;
        try {
            value = type.parse(reader);
        } catch (CommandSyntaxException | RuntimeException e) {
            return -1;
        }
        int valueEnd = reader.getCursor();
        if (valueEnd <= pos || valueEnd > end) {
            return -1;
        }
        arguments.put(node.getName(), new ParsedArgument<>(pos, valueEnd, value));
        nodes.add(new ParsedCommandNode<>(node, StringRange.between(pos, valueEnd)));
        return valueEnd;
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

    /**
     * A state: the counts of the options given so far and the argument node they follow, or null before the first
     * argument. The position is compared by identity, since a node's equality compares its whole subtree.
     */
    private final class State {
        private final Map<CommandOption<?, C>, Integer> counts;
        private final @Nullable CommandNode<S> position;

        private State(Map<CommandOption<?, C>, Integer> counts, @Nullable CommandNode<S> position) {
            this.counts = counts;
            this.position = position;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (!(o instanceof OptionGraph.State)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            State other = (State) o;
            return position == other.position && counts.equals(other.counts);
        }

        @Override
        public int hashCode() {
            return 31 * counts.hashCode() + System.identityHashCode(position);
        }
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

    private final class Transition {
        // The option whose value follows the literal, or null for flags.
        private final @Nullable CommandOption<?, C> valueOption;
        private final State next;
        private final CommandNode<S> literal;
        private final @Nullable CommandNode<S> valueNode;

        private Transition(@Nullable CommandOption<?, C> valueOption,
                           State next,
                           CommandNode<S> literal,
                           @Nullable CommandNode<S> valueNode) {
            this.valueOption = valueOption;
            this.next = next;
            this.literal = literal;
            this.valueNode = valueNode;
        }
    }

    private final class Region {
        private final int start;
        private final List<ParsedCommandNode<S>> nodes;
        private final Map<String, ParsedArgument<S, ?>> arguments;

        private Region(int start, List<ParsedCommandNode<S>> nodes, Map<String, ParsedArgument<S, ?>> arguments) {
            this.start = start;
            this.nodes = nodes;
            this.arguments = arguments;
        }
    }
}
