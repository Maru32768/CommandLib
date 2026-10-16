package dev.marulabs.commandlib;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.*;
import java.util.function.Predicate;
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
            node.addChild(createHelpNode(command, inheritedArguments, helpAction));
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

    // A runner without an executor sends the help after the same checks as the command, and reports its failures.
    private LiteralCommandNode<S> createHelpNode(U command,
                                                 List<Arguments<C>> inheritedArguments,
                                                 HelpMessageAction<S, T, C, U> helpAction) {
        String permission = command.permissionName(permissionPrefix);
        CommandRunner<S, C> runner = new CommandRunner<>(platformAdapter,
                                                         scheduler,
                                                         command,
                                                         permissionPrefix,
                                                         inheritedArguments,
                                                         OptionGraph.of(platformAdapter,
                                                                        command,
                                                                        inheritedArguments,
                                                                        permissionPrefix),
                                                         command.prerequisite(),
                                                         helpAction,
                                                         command.middlewares(),
                                                         null,
                                                         command.uncaughtExceptionHandlers());
        return LiteralArgumentBuilder.<S>literal("help")
                                     .requires(x -> platformAdapter.hasPermission(x, permission))
                                     .executes(runner)
                                     .build();
    }

    private LiteralCommandNode<S> toCommandNode(U command,
                                                List<Arguments<C>> inheritedArguments,
                                                HelpMessageAction<S, T, C, U> helpAction) {
        LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.literal(command.name());
        builder.requires(requirement(command));

        List<Arguments<C>> argumentsList = command.argumentsList();
        OptionGraph<S, C> optionGraph = OptionGraph.of(platformAdapter, command, inheritedArguments, permissionPrefix);

        if (argumentsList.isEmpty()) {
            CommandRunner<S, C> runner = new CommandRunner<>(platformAdapter,
                                                             scheduler,
                                                             command,
                                                             permissionPrefix,
                                                             inheritedArguments,
                                                             optionGraph,
                                                             command.prerequisite(),
                                                             helpAction,
                                                             command.middlewares(),
                                                             command.executor(),
                                                             command.uncaughtExceptionHandlers());
            builder.executes(runner);
            LiteralCommandNode<S> node = builder.build();
            optionGraph.build(List.of(), runner)
                       .forEach(node::addChild);
            return node;
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
                                      optionGraph,
                                      command.prerequisite(),
                                      helpAction,
                                      command.middlewares(),
                                      command.executor(),
                                      command.uncaughtExceptionHandlers())
                : new ArgumentCommandNodeCreator<>(allOptional.get(0),
                                                   argumentsList,
                                                   appendArgument(inheritedArguments, allOptional.get(0)),
                                                   optionGraph,
                                                   permissionPrefix,
                                                   scheduler).runner(allOptional.get(0)
                                                                                .first(),
                                                                     helpAction,
                                                                     command);
        builder.executes(bareRunner);
        LiteralCommandNode<S> node = builder.build();

        // The argument nodes of every branch are built once and follow the command literal and every option state.
        // They are merged as the command literal would merge them, so branches whose first arguments share a name
        // become one node before the option states take them.
        LiteralCommandNode<S> argumentNodes = LiteralArgumentBuilder.<S>literal(command.name())
                                                                    .build();
        argumentsList.stream()
                     .sorted((x, y) -> Integer.compare(y.size(), x.size())) // Sort in descending order to handle variable-length arguments
                     .forEach(arguments -> {
                         List<Arguments<C>> executorArguments = appendArgument(inheritedArguments, arguments);
                         // Brigadier can attach literal nodes under an argument node. The executor still needs the
                         // parent arguments, so child commands inherit the complete argument chain here.
                         List<LiteralCommandNode<S>> childNodes = arguments.children()
                                                                           .stream()
                                                                           .map(x -> toCommandNodes(castCommand(x),
                                                                                                    executorArguments))
                                                                           .flatMap(Collection::stream)
                                                                           .collect(Collectors.toList());
                         argumentNodes.addChild(new ArgumentCommandNodeCreator<>(arguments,
                                                                                 argumentsList,
                                                                                 executorArguments,
                                                                                 optionGraph,
                                                                                 permissionPrefix,
                                                                                 scheduler).build(helpAction,
                                                                                                  command,
                                                                                                  childNodes));
                     });

        optionGraph.build(new ArrayList<>(argumentNodes.getChildren()), bareRunner)
                   .forEach(node::addChild);
        return node;
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

    // The sender has the command's permission and the command is visible to the sender.
    private Predicate<S> requirement(U command) {
        String permission = command.permissionName(permissionPrefix);
        if (!command.hasVisibility()) {
            return x -> platformAdapter.hasPermission(x, permission);
        }
        return x -> platformAdapter.hasPermission(x, permission)
                && command.isVisible(platformAdapter.createSourceContext(x));
    }

    private List<LiteralCommandNode<S>> createAliasCommands(U source, LiteralCommandNode<S> redirectTarget) {
        Predicate<S> requirement = requirement(source);
        return source.aliases()
                     .stream()
                     .map(s -> {
                         LiteralCommandNode<S> node = LiteralArgumentBuilder.<S>literal(s)
                                                                            .requires(requirement)
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
