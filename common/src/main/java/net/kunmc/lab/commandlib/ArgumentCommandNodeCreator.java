package net.kunmc.lab.commandlib;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.suggestion.SuggestionBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

final class ArgumentCommandNodeCreator<S, T, C extends CommonCommandContext<S, T>> {
    private final PlatformAdapter<S, T, C, ?> platformAdapter = PlatformAdapter.get();
    private final Arguments<C> arguments;
    private final List<Arguments<C>> executorArguments;
    private final String permissionPrefix;

    ArgumentCommandNodeCreator(Arguments<C> arguments, List<Arguments<C>> executorArguments, String permissionPrefix) {
        this.arguments = arguments;
        this.executorArguments = List.copyOf(executorArguments);
        this.permissionPrefix = permissionPrefix;
    }

    private RequiredArgumentBuilder<S, ?> buildArgument(CommonArgument<?, C, ?> argument,
                                                        CommandExecutor<C> helpAction,
                                                        CommonCommand<C, ?> parent) {
        RequiredArgumentBuilder<S, ?> builder = RequiredArgumentBuilder.argument(argument.name(), argument.type());
        builder.requires(source -> platformAdapter.hasPermission(source,
                                                                 arguments.permissionName(parent, permissionPrefix)));

        builder.suggests((context, sb) -> {
            try {
                if (!platformAdapter.hasPermission(context.getSource(),
                                                   arguments.permissionName(parent, permissionPrefix))) {
                    return Suggestions.empty();
                }
                C ctx = platformAdapter.createCommandContext(context);
                SuggestionBuilder<C> suggestionBuilder = new SuggestionBuilder<>(ctx, sb.getRemaining());
                List<CompletableFuture<Suggestions>> defaultFutures = new ArrayList<>();
                // Parsing runs user code (validators, transformers) too, so it shares the current context with the
                // suggestion actions, matching CommandRunner.
                CurrentCommandContext.run(ctx, () -> {
                    // Include inherited parent arguments so argument child commands can read them while suggesting.
                    for (Arguments<C> parsed : executorArguments) {
                        try {
                            parsed.parse(ctx);
                        } catch (Exception ignored) {
                            // Best-effort: pre-populate ctx with already-parsed arguments for use in
                            // suggestion actions. Failures (e.g. platform-specific context differences)
                            // are non-fatal - the suggestion action still runs.
                        }
                    }

                    for (ArgumentSuggestionAction<C> suggestionAction : argument.suggestionActions()) {
                        if (suggestionAction.isDefaultSuggestions()) {
                            defaultFutures.add(argument.type()
                                                       .listSuggestions(context, sb)
                                                       .toCompletableFuture());
                        } else {
                            suggestionAction.action()
                                            .accept(suggestionBuilder);
                        }
                    }
                });

                CompletableFuture<Void> customFuture = suggestionBuilder.awaitAll()
                                                                        .toCompletableFuture()
                                                                        .thenRun(() -> {
                                                                            suggestionBuilder.build()
                                                                                             .forEach(s -> s.suggest(sb));
                                                                        });
                CompletableFuture<Void> defaultFuture = CompletableFuture.allOf(defaultFutures.toArray(CompletableFuture[]::new));

                return customFuture.thenCombine(defaultFuture, (ignored, suggestions) -> sb.build());
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        });

        builder.executes(new CommandRunner<>(platformAdapter,
                                             parent,
                                             permissionPrefix,
                                             executorArguments,
                                             parent.options(),
                                             parent.prerequisite(),
                                             helpAction,
                                             parent.preprocess(),
                                             argument.executor(),
                                             argument.uncaughtExceptionHandlers()));

        return builder;
    }

    private List<ArgumentCommandNode<S, ?>> toCommandNodes(CommandExecutor<C> helpAction, CommonCommand<C, ?> parent) {
        return arguments.stream()
                        .map(x -> buildArgument(x, helpAction, parent))
                        .map(RequiredArgumentBuilder::build)
                        .collect(Collectors.toList());
    }

    ArgumentCommandNode<S, ?> build(CommandExecutor<C> helpAction,
                                    CommonCommand<C, ?> parent,
                                    Collection<LiteralCommandNode<S>> terminalChildren) {
        List<ArgumentCommandNode<S, ?>> nodes = toCommandNodes(helpAction, parent);
        if (nodes.isEmpty()) {
            return null;
        }

        for (int i = 0; i < nodes.size() - 1; i++) {
            nodes.get(i)
                 .addChild(nodes.get(i + 1));
        }

        for (var child : terminalChildren) {
            // Append sub command nodes
            nodes.get(nodes.size() - 1)
                 .addChild(child);
        }

        return nodes.get(0);
    }
}
