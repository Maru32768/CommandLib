package net.kunmc.lab.commandlib;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class CommandNodeCreator<S, T, C extends CommonCommandContext<S, T>, U extends CommonCommand<C, U>> {
    private final PlatformAdapter<S, T, C, U> platformAdapter = PlatformAdapter.get();
    private final Collection<? extends U> commands;
    private final String permissionPrefix;

    public CommandNodeCreator(Collection<? extends U> commands, String permissionPrefix) {
        this.commands = commands;
        this.permissionPrefix = permissionPrefix;
    }

    public List<LiteralCommandNode<S>> build() {
        validateUniqueLiterals(null, commands);
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

        validateUniqueLiterals(command.name(), command.children());
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

    private static void validateUniqueLiterals(String parentName, Collection<? extends CommonCommand<?, ?>> commands) {
        // Brigadier silently merges sibling literals with the same name, which would make one command unreachable.
        // Repeating a name within one command, or adding the same command twice, merges identical nodes and is
        // harmless, so only collisions between different commands are rejected.
        Map<String, CommonCommand<?, ?>> owners = new HashMap<>();
        for (CommonCommand<?, ?> command : commands) {
            List<String> names = new ArrayList<>();
            names.add(command.name());
            names.addAll(command.aliases());
            for (String name : names) {
                CommonCommand<?, ?> owner = owners.putIfAbsent(name, command);
                if (owner != null && owner != command) {
                    throw new IllegalStateException(parentName == null ? "Duplicate command name or alias '" + name + "'" : "Duplicate child command name or alias '" + name + "' under '" + parentName + "'");
                }
            }
        }
    }

    private boolean shouldAddHelpChild(U command) {
        if (command.children()
                   .isEmpty() && command.argumentsList()
                                        .isEmpty() && command.options()
                                                             .isEmpty()) {
            return false;
        }
        // A child named or aliased "help" replaces the generated help node; adding both would make Brigadier merge
        // the two literals.
        return command.children()
                      .stream()
                      .noneMatch(x -> x.name()
                                       .equals("help") || x.aliases()
                                                           .contains("help"));
    }

    private LiteralCommandNode<S> createHelpNode(U command, HelpMessageAction<S, T, C, U> helpAction) {
        return LiteralArgumentBuilder.<S>literal("help")
                                     .requires(x -> platformAdapter.hasPermission(command, x, permissionPrefix))
                                     .executes(context -> {
                                         try {
                                             C ctx = platformAdapter.createCommandContext(context);
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
                                                             command,
                                                             permissionPrefix,
                                                             inheritedArguments,
                                                             command.options(),
                                                             command.prerequisite(),
                                                             helpAction,
                                                             command.preprocess(),
                                                             command.executor(),
                                                             command.uncaughtExceptionHandlers());
            builder.executes(runner);
            createOptionCommands(command.options(), () -> null, runner).forEach(builder::then);
            return builder.build();
        }

        // Input that stops at the command literal (optionally followed by options only) consumes no branch, so its
        // runner must not check or parse any branch of this command. Using a branch runner here would require that
        // branch's permission for a bare "/command" call.
        CommandRunner<S, C> bareRunner = new CommandRunner<>(platformAdapter,
                                                             command,
                                                             permissionPrefix,
                                                             inheritedArguments,
                                                             command.options(),
                                                             command.prerequisite(),
                                                             helpAction,
                                                             command.preprocess(),
                                                             command.executor(),
                                                             command.uncaughtExceptionHandlers());
        builder.executes(bareRunner);

        argumentsList.stream()
                     .sorted((x, y) -> Integer.compare(y.size(),
                                                       x.size())) // Sort in descending order to handle variable-length arguments
                     .forEach(arguments -> {
                         List<Arguments<C>> executorArguments = appendArgument(inheritedArguments, arguments);
                         // Brigadier can attach literal nodes under an argument node. The executor still needs the
                         // parent arguments, so child commands inherit the complete argument chain here. The child
                         // nodes are built once and shared by every argument node the supplier creates for options,
                         // as alias nodes share the children of their target.
                         validateUniqueLiterals(command.name(), arguments.children());
                         List<LiteralCommandNode<S>> childNodes = arguments.children()
                                                                           .stream()
                                                                           .map(x -> toCommandNodes(castCommand(x),
                                                                                                    executorArguments))
                                                                           .flatMap(Collection::stream)
                                                                           .collect(Collectors.toList());
                         Supplier<ArgumentCommandNode<S, ?>> argumentNodeSupplier = () -> new ArgumentCommandNodeCreator<>(
                                 arguments,
                                 executorArguments,
                                 permissionPrefix).build(helpAction, command, childNodes);
                         builder.then(argumentNodeSupplier.get());
                         createOptionCommands(command.options(), argumentNodeSupplier, bareRunner).forEach(builder::then);
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

    private List<CommandNode<S>> createOptionCommands(List<CommandOption<?, C>> options,
                                                      Supplier<ArgumentCommandNode<S, ?>> argumentNodeSupplier,
                                                      CommandRunner<S, C> executor) {
        if (options.isEmpty()) {
            return List.of();
        }

        return createOptionCommands(options, new HashSet<>(), argumentNodeSupplier, executor);
    }

    private List<CommandNode<S>> createOptionCommands(List<CommandOption<?, C>> options,
                                                      Set<CommandOption<?, C>> selectedOptions,
                                                      Supplier<ArgumentCommandNode<S, ?>> argumentNodeSupplier,
                                                      CommandRunner<S, C> executor) {
        List<CommandNode<S>> nodes = new ArrayList<>();

        for (OptionToken<C> optionToken : createOptionTokens(options, selectedOptions)) {
            List<CommandOption<?, C>> tokenOptions = optionToken.options();
            Set<CommandOption<?, C>> nextSelectedOptions = new HashSet<>(selectedOptions);
            nextSelectedOptions.addAll(tokenOptions);

            LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.literal(optionToken.token());
            if (!isValueOptionToken(tokenOptions)) {
                builder.executes(executor);
            }

            addArgumentAndOptionChildren(builder,
                                         tokenOptions,
                                         argumentNodeSupplier.get(),
                                         options,
                                         nextSelectedOptions,
                                         argumentNodeSupplier,
                                         executor);

            nodes.add(builder.build());
        }

        return nodes;
    }

    private void addArgumentAndOptionChildren(LiteralArgumentBuilder<S> builder,
                                              List<CommandOption<?, C>> tokenOptions,
                                              ArgumentCommandNode<S, ?> argumentNode,
                                              List<CommandOption<?, C>> options,
                                              Set<CommandOption<?, C>> nextSelectedOptions,
                                              Supplier<ArgumentCommandNode<S, ?>> argumentNodeSupplier,
                                              CommandRunner<S, C> executor) {
        if (isValueOptionToken(tokenOptions)) {
            CommandOption<?, C> option = tokenOptions.get(0);
            RequiredArgumentBuilder<S, ?> valueBuilder = RequiredArgumentBuilder.argument(option.internalName(),
                                                                                          option.type());
            valueBuilder.executes(executor);
            if (argumentNode != null) {
                valueBuilder.then(argumentNode);
            }
            createOptionCommands(options,
                                 nextSelectedOptions,
                                 argumentNodeSupplier,
                                 executor).forEach(valueBuilder::then);
            builder.then(valueBuilder);
            return;
        }

        if (argumentNode != null) {
            builder.then(argumentNode);
        }
        createOptionCommands(options, nextSelectedOptions, argumentNodeSupplier, executor).forEach(builder::then);
    }

    private boolean isValueOptionToken(List<CommandOption<?, C>> tokenOptions) {
        return tokenOptions.size() == 1 && tokenOptions.get(0)
                                                       .hasValue();
    }

    private List<OptionToken<C>> createOptionTokens(List<CommandOption<?, C>> options,
                                                    Set<CommandOption<?, C>> selectedOptions) {
        List<OptionToken<C>> optionTokens = new ArrayList<>();
        List<CommandOption<?, C>> remainingOptions = options.stream()
                                                            .filter(x -> !selectedOptions.contains(x))
                                                            .collect(Collectors.toList());

        for (CommandOption<?, C> option : remainingOptions) {
            optionTokens.add(new OptionToken<>("--" + option.name(), List.of(option)));
            if (option.shortName() != null) {
                optionTokens.add(new OptionToken<>("-" + option.shortName(), List.of(option)));
            }
        }

        List<CommandOption<?, C>> shortOptions = remainingOptions.stream()
                                                                 .filter(x -> x.shortName() != null && !x.hasValue())
                                                                 .collect(Collectors.toList());
        for (int size = 2; size <= shortOptions.size(); size++) {
            collectShortOptionPermutations(shortOptions, size, new ArrayList<>(), optionTokens);
        }

        return optionTokens;
    }

    private void collectShortOptionPermutations(List<CommandOption<?, C>> options,
                                                int size,
                                                List<CommandOption<?, C>> current,
                                                List<OptionToken<C>> dest) {
        if (current.size() == size) {
            dest.add(new OptionToken<>("-" + current.stream()
                                                    .map(CommandOption::shortName)
                                                    .map(String::valueOf)
                                                    .collect(Collectors.joining()), List.copyOf(current)));
            return;
        }

        for (CommandOption<?, C> option : options) {
            if (current.contains(option)) {
                continue;
            }
            current.add(option);
            collectShortOptionPermutations(options, size, current, dest);
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
                                                                            .requires(x -> platformAdapter.hasPermission(
                                                                                    source,
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
