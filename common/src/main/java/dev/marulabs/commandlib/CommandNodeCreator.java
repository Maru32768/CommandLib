package dev.marulabs.commandlib;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.marulabs.commandlib.internal.PlatformAdapter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class CommandNodeCreator<S, T, C extends CommonCommandContext<S, T>, U extends CommonCommand<C, U>> {
    private final PlatformAdapter<S, T, C> platformAdapter = PlatformAdapter.get();
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
        CommandTree.validatePermissions(commands, permissionPrefix);
        return commands.stream()
                       .map(x -> toCommandNodes(x, List.of()))
                       .flatMap(Collection::stream)
                       .collect(Collectors.toList());
    }

    private List<LiteralCommandNode<S>> toCommandNodes(U command, List<Arguments<C>> inheritedArguments) {
        HelpMessageAction<S, T, C> helpAction = new HelpMessageAction<>(platformAdapter, command, permissionPrefix);
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
        // A child named or aliased "help", or a first argument named so, replaces the generated help node; adding
        // both would make Brigadier merge the two.
        return command.children()
                      .stream()
                      .noneMatch(x -> x.name()
                                       .equals("help")
                              || x.aliases()
                                  .contains("help"))
                && command.argumentsList()
                          .stream()
                          .noneMatch(x -> x.stream()
                                           .findFirst()
                                           .map(argument -> argument.name()
                                                                    .equals("help"))
                                           .orElse(false));
    }

    // A runner without an executor sends the help after the same checks as the command, and reports its failures.
    private LiteralCommandNode<S> createHelpNode(U command,
                                                 List<Arguments<C>> inheritedArguments,
                                                 HelpMessageAction<S, T, C> helpAction) {
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
                                                HelpMessageAction<S, T, C> helpAction) {
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
        argumentsList.forEach(x -> validateNamesAlongRoute(command, inheritedArguments, x));
        List<Arguments<C>> allOptional = argumentsList.stream()
                                                      .filter(Arguments::isAllOptional)
                                                      .collect(Collectors.toList());
        if (allOptional.size() > 1 || (allOptional.size() == 1 && command.executor() != null)) {
            throw new IllegalStateException("Command '" + command.name()
                    + "' has more than one action for input without arguments: its own executor or a branch whose arguments are all optional");
        }
        // Brigadier keeps one node for the same argument names, so of two branches that run for input ending there,
        // the order of definition would silently pick one.
        Map<List<String>, Arguments<C>> runsAt = new HashMap<>();
        for (Arguments<C> arguments : argumentsList) {
            List<String> names = new ArrayList<>();
            for (CommonArgument<?, C, ?> argument : arguments.stream()
                                                             .collect(Collectors.toList())) {
                names.add(argument.name());
                if (arguments.executorFor(argument) == null) {
                    continue;
                }
                Arguments<C> other = runsAt.putIfAbsent(List.copyOf(names), arguments);
                if (other != null && other != arguments) {
                    throw new IllegalStateException("Command '" + command.name()
                            + "' has more than one argument branch that runs for input ending at <"
                            + String.join("> <", names) + ">");
                }
            }
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
                                                   scheduler).bareRunner(helpAction, command);
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

    // The context keeps the values of a route by argument name, so a repeated name would replace the earlier value.
    private static <C extends CommonCommandContext<?, ?>> void validateNamesAlongRoute(CommonCommand<C, ?> command,
                                                                                       List<Arguments<C>> inheritedArguments,
                                                                                       Arguments<C> arguments) {
        Set<String> inherited = inheritedArguments.stream()
                                                  .flatMap(Arguments::stream)
                                                  .map(CommonArgument::name)
                                                  .collect(Collectors.toSet());
        arguments.stream()
                 .filter(x -> inherited.contains(x.name()))
                 .findFirst()
                 .ifPresent(x -> {
                     throw new IllegalStateException("Argument '" + x.name() + "' of command '" + route(command)
                             + "' has the name of an argument before it on its route; argument names must be unique along a route");
                 });
    }

    // The route by names, such as /config <key> set.
    private static String route(CommonCommand<?, ?> command) {
        CommonCommand<?, ?> parent = command.parent();
        if (parent == null) {
            return "/" + command.name();
        }
        Arguments<?> parentArguments = command.parentArguments();
        String tags = parentArguments == null ? "" : parentArguments.stream()
                                                                    .map(x -> " <" + x.name() + ">")
                                                                    .collect(Collectors.joining());
        return route(parent) + tags + " " + command.name();
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
                && command.isVisibleTo(platformAdapter.createSourceContext(x));
    }

    private List<LiteralCommandNode<S>> createAliasCommands(U source, LiteralCommandNode<S> redirectTarget) {
        Predicate<S> requirement = requirement(source);
        // An alias node named like the command would merge into it and make its command call itself.
        return source.aliases()
                     .stream()
                     .filter(x -> !x.equals(source.name()))
                     .distinct()
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
