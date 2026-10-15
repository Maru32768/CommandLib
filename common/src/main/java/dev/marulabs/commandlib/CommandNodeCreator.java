package dev.marulabs.commandlib;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.marulabs.commandlib.command.Extractor;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class CommandNodeCreator<S, T, C extends CommonCommandContext<S, T>, U extends CommonCommand<C, U>> {
    private final PlatformAdapter<S, T, C, U> platformAdapter = PlatformAdapter.get();
    private final Collection<? extends U> commands;
    private final String permissionPrefix;
    private final CommandScheduler scheduler;

    public CommandNodeCreator(Collection<? extends U> commands, String permissionPrefix, CommandScheduler scheduler) {
        this.commands = commands;
        this.permissionPrefix = permissionPrefix;
        this.scheduler = scheduler;
    }

    public List<LiteralCommandNode<S>> build() {
        CommandNameValidator.validateTopLevel(commands);
        return commands.stream()
                       .map(x -> toCommandNodes(x, List.of()))
                       .flatMap(Collection::stream)
                       .collect(Collectors.toList());
    }

    private List<LiteralCommandNode<S>> toCommandNodes(U command, List<Arguments<C>> inheritedArguments) {
        HelpMessageAction<S, T, C, U> helpAction = new HelpMessageAction<>(platformAdapter, command, permissionPrefix);
        List<LiteralCommandNode<S>> nodes = new ArrayList<>();
        LiteralCommandNode<S> node = toCommandNode(command, inheritedArguments, helpAction);
        nodes.add(node);

        command.children()
               .forEach(x -> {
                   // Normal command children do not consume new arguments, so they must keep the same inherited
                   // argument chain that was parsed before this command.
                   toCommandNodes(x, inheritedArguments).forEach(node::addChild);
               });

        if (shouldAddHelpChild(command)) {
            node.addChild(createHelpNode(command, helpAction));
        }

        nodes.addAll(createAliasCommands(command, node));

        return nodes;
    }

    private boolean shouldAddHelpChild(U command) {
        if (command.children()
                   .isEmpty()
                && command.argumentsList()
                          .isEmpty()
                && command.options()
                          .isEmpty()) {
            return false;
        }
        // A child named or aliased "help" replaces the generated help node; adding both would make Brigadier merge
        // the two literals.
        return command.children()
                      .stream()
                      .noneMatch(x -> x.name()
                                       .equals("help")
                              || x.aliases()
                                  .contains("help"));
    }

    private LiteralCommandNode<S> createHelpNode(U command, HelpMessageAction<S, T, C, U> helpAction) {
        Map<ContextKey<?>, ? extends Extractor<?, ?>> providers = command.providers();
        return LiteralArgumentBuilder.<S>literal("help")
                                     .requires(x -> platformAdapter.hasPermission(command, x, permissionPrefix))
                                     .executes(context -> {
                                         try {
                                             C ctx = platformAdapter.createCommandContext(context);
                                             ctx.providers(providers);
                                             CurrentCommandContext.run(ctx, () -> helpAction.accept(ctx));
                                             return 1;
                                         } catch (Exception e) {
                                             e.printStackTrace();
                                             return 0;
                                         }
                                     })
                                     .build();
    }

    private LiteralCommandNode<S> toCommandNode(U command,
                                                List<Arguments<C>> inheritedArguments,
                                                HelpMessageAction<S, T, C, U> helpAction) {
        LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.literal(command.name());
        builder.requires(x -> platformAdapter.hasPermission(command, x, permissionPrefix));

        List<Arguments<C>> argumentsList = command.argumentsList();

        if (argumentsList.isEmpty()) {
            CommandRunner<S, C> runner = new CommandRunner<>(platformAdapter,
                                                             scheduler,
                                                             command,
                                                             permissionPrefix,
                                                             inheritedArguments,
                                                             command.options(),
                                                             command.prerequisite(),
                                                             helpAction,
                                                             command.middlewares(),
                                                             command.executor(),
                                                             command.uncaughtExceptionHandlers());
            builder.executes(runner);
            createOptionCommands(command, () -> null, runner).forEach(builder::then);
            return builder.build();
        }

        argumentsList.forEach(x -> x.validateTrailingArguments(command));
        List<Arguments<C>> allOptional = argumentsList.stream()
                                                      .filter(Arguments::isAllOptional)
                                                      .collect(Collectors.toList());
        if (allOptional.size() > 1 || (allOptional.size() == 1 && command.executor() != null)) {
            throw new IllegalStateException("Command '" + command.name()
                    + "' has more than one action for input without arguments: its own executor or a branch whose arguments are all optional");
        }

        // Input that stops at the command literal (optionally followed by options only) consumes no branch, so its
        // runner must not check or parse any branch of this command. Using a branch runner here would require that
        // branch's permission for a bare "/command" call. A branch whose arguments are all optional is the exception:
        // it runs with its defaults then.
        CommandRunner<S, C> bareRunner = allOptional.isEmpty()
                ? new CommandRunner<>(platformAdapter,
                                      scheduler,
                                      command,
                                      permissionPrefix,
                                      inheritedArguments,
                                      command.options(),
                                      command.prerequisite(),
                                      helpAction,
                                      command.middlewares(),
                                      command.executor(),
                                      command.uncaughtExceptionHandlers())
                : new ArgumentCommandNodeCreator<>(allOptional.get(0),
                                                   appendArgument(inheritedArguments, allOptional.get(0)),
                                                   permissionPrefix,
                                                   scheduler).runner(allOptional.get(0)
                                                                                .first(),
                                                                     helpAction,
                                                                     command);
        builder.executes(bareRunner);

        argumentsList.stream()
                     .sorted((x, y) -> Integer.compare(y.size(), x.size())) // Sort in descending order to handle variable-length arguments
                     .forEach(arguments -> {
                         List<Arguments<C>> executorArguments = appendArgument(inheritedArguments, arguments);
                         // Brigadier can attach literal nodes under an argument node. The executor still needs the
                         // parent arguments, so child commands inherit the complete argument chain here. The child
                         // nodes are built once and shared by every argument node the supplier creates for options,
                         // as alias nodes share the children of their target.
                         List<LiteralCommandNode<S>> childNodes = arguments.children()
                                                                           .stream()
                                                                           .map(x -> toCommandNodes(castCommand(x),
                                                                                                    executorArguments))
                                                                           .flatMap(Collection::stream)
                                                                           .collect(Collectors.toList());
                         Supplier<ArgumentCommandNode<S, ?>> argumentNodeSupplier = () -> new ArgumentCommandNodeCreator<>(arguments,
                                                                                                                           executorArguments,
                                                                                                                           permissionPrefix,
                                                                                                                           scheduler).build(helpAction,
                                                                                                                                            command,
                                                                                                                                            childNodes);
                         builder.then(argumentNodeSupplier.get());
                         createOptionCommands(command, argumentNodeSupplier, bareRunner).forEach(builder::then);
                     });

        return builder.build();
    }

    private List<Arguments<C>> appendArgument(List<Arguments<C>> inheritedArguments, Arguments<C> arguments) {
        List<Arguments<C>> result = new ArrayList<>(inheritedArguments);
        result.add(arguments);
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private U castCommand(CommonCommand<C, ?> command) {
        return (U) command;
    }

    private List<CommandNode<S>> createOptionCommands(U command,
                                                      Supplier<ArgumentCommandNode<S, ?>> argumentNodeSupplier,
                                                      CommandRunner<S, C> executor) {
        if (command.options()
                   .isEmpty()) {
            return List.of();
        }

        return createOptionCommands(command, Map.of(), argumentNodeSupplier, executor, new HashMap<>());
    }

    // usedOptions counts the occurrences of each option on the path to the nodes being created. The nodes after a
    // path depend only on those counts, so every path with the same counts shares them through childrenByUsedOptions.
    // Otherwise the tree would grow with every order the options can be given in.
    private List<CommandNode<S>> createOptionCommands(U command,
                                                      Map<CommandOption<?, C>, Integer> usedOptions,
                                                      Supplier<ArgumentCommandNode<S, ?>> argumentNodeSupplier,
                                                      CommandRunner<S, C> executor,
                                                      Map<Map<CommandOption<?, C>, Integer>, List<CommandNode<S>>> childrenByUsedOptions) {
        List<CommandNode<S>> nodes = new ArrayList<>();

        for (OptionToken<C> optionToken : createOptionTokens(command, usedOptions)) {
            List<CommandOption<?, C>> tokenOptions = optionToken.options();
            Map<CommandOption<?, C>, Integer> nextUsedOptions = new HashMap<>(usedOptions);
            tokenOptions.forEach(x -> nextUsedOptions.merge(x, 1, Integer::sum));
            List<CommandNode<S>> children = createOptionChildren(command,
                                                                 nextUsedOptions,
                                                                 argumentNodeSupplier,
                                                                 executor,
                                                                 childrenByUsedOptions);

            LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.literal(optionToken.token());
            if (isValueOptionToken(tokenOptions)) {
                CommandOption<?, C> option = tokenOptions.get(0);
                // Each occurrence needs its own argument name, since Brigadier keeps one value per name.
                RequiredArgumentBuilder<S, ?> valueBuilder = RequiredArgumentBuilder.argument(option.internalName(usedOptions.getOrDefault(option,
                                                                                                                                           0)),
                                                                                              option.type());
                valueBuilder.executes(executor);
                children.forEach(valueBuilder::then);
                builder.then(valueBuilder);
            } else {
                builder.executes(executor);
                children.forEach(builder::then);
            }

            nodes.add(builder.build());
        }

        return nodes;
    }

    // The nodes that may follow the options counted by usedOptions: the argument node and the remaining options.
    private List<CommandNode<S>> createOptionChildren(U command,
                                                      Map<CommandOption<?, C>, Integer> usedOptions,
                                                      Supplier<ArgumentCommandNode<S, ?>> argumentNodeSupplier,
                                                      CommandRunner<S, C> executor,
                                                      Map<Map<CommandOption<?, C>, Integer>, List<CommandNode<S>>> childrenByUsedOptions) {
        List<CommandNode<S>> cached = childrenByUsedOptions.get(usedOptions);
        if (cached != null) {
            return cached;
        }

        List<CommandNode<S>> children = new ArrayList<>();
        ArgumentCommandNode<S, ?> argumentNode = argumentNodeSupplier.get();
        if (argumentNode != null) {
            children.add(argumentNode);
        }
        children.addAll(createOptionCommands(command,
                                             usedOptions,
                                             argumentNodeSupplier,
                                             executor,
                                             childrenByUsedOptions));
        childrenByUsedOptions.put(usedOptions, children);
        return children;
    }

    private boolean isValueOptionToken(List<CommandOption<?, C>> tokenOptions) {
        return tokenOptions.size() == 1 && tokenOptions.get(0)
                                                       .hasValue();
    }

    private List<OptionToken<C>> createOptionTokens(U command, Map<CommandOption<?, C>, Integer> usedOptions) {
        List<OptionToken<C>> optionTokens = new ArrayList<>();
        List<CommandOption<?, C>> remainingOptions = command.options()
                                                            .stream()
                                                            .filter(x -> usedOptions.getOrDefault(x,
                                                                                                  0) < x.maxOccurrences())
                                                            .filter(x -> usedOptions.keySet()
                                                                                    .stream()
                                                                                    .noneMatch(y -> command.excludes(x,
                                                                                                                     y)))
                                                            .collect(Collectors.toList());

        for (CommandOption<?, C> option : remainingOptions) {
            for (String name : option.names()) {
                optionTokens.add(new OptionToken<>("--" + name, List.of(option)));
            }
            Character shortName = option.shortName();
            if (shortName == null) {
                continue;
            }
            optionTokens.add(new OptionToken<>("-" + shortName, List.of(option)));
            if (option.hasValue()) {
                continue;
            }
            // A counted flag can repeat its short name in one token, such as -vvv.
            int remaining = option.maxOccurrences() - usedOptions.getOrDefault(option, 0);
            for (int count = 2; count <= remaining; count++) {
                optionTokens.add(new OptionToken<>("-" + String.valueOf(shortName)
                                                               .repeat(count),
                                                   Collections.nCopies(count, option)));
            }
        }

        List<CommandOption<?, C>> shortOptions = remainingOptions.stream()
                                                                 .filter(x -> x.shortName() != null && !x.hasValue())
                                                                 .collect(Collectors.toList());
        for (int size = 2; size <= shortOptions.size(); size++) {
            collectShortOptionPermutations(command, shortOptions, size, new ArrayList<>(), optionTokens);
        }

        return optionTokens;
    }

    private void collectShortOptionPermutations(U command,
                                                List<CommandOption<?, C>> options,
                                                int size,
                                                List<CommandOption<?, C>> current,
                                                List<OptionToken<C>> dest) {
        if (current.size() == size) {
            dest.add(new OptionToken<>("-" + current.stream()
                                                    .map(CommandOption::shortName)
                                                    .map(String::valueOf)
                                                    .collect(Collectors.joining()),
                                       List.copyOf(current)));
            return;
        }

        for (CommandOption<?, C> option : options) {
            if (current.contains(option) || current.stream()
                                                   .anyMatch(x -> command.excludes(x, option))) {
                continue;
            }
            current.add(option);
            collectShortOptionPermutations(command, options, size, current, dest);
            current.remove(current.size() - 1);
        }
    }

    private static class OptionToken<C extends CommonCommandContext<?, ?>> {
        private final String token;
        private final List<CommandOption<?, C>> options;

        private OptionToken(String token, List<CommandOption<?, C>> options) {
            this.token = token;
            this.options = options;
        }

        private String token() {
            return token;
        }

        private List<CommandOption<?, C>> options() {
            return options;
        }
    }

    private List<LiteralCommandNode<S>> createAliasCommands(U source, LiteralCommandNode<S> redirectTarget) {
        return source.aliases()
                     .stream()
                     .map(s -> {
                         LiteralCommandNode<S> node = LiteralArgumentBuilder.<S>literal(s)
                                                                            .requires(x -> platformAdapter.hasPermission(source,
                                                                                                                         x,
                                                                                                                         permissionPrefix))
                                                                            .executes(x -> redirectTarget.getCommand()
                                                                                                         .run(x))
                                                                            .build();
                         redirectTarget.getChildren()
                                       .forEach(node::addChild);
                         return node;
                     })
                     .collect(Collectors.toList());
    }

}
