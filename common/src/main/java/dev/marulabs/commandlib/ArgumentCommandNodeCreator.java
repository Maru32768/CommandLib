package dev.marulabs.commandlib;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.suggestion.SuggestionBuilder;
import dev.marulabs.commandlib.suggestion.SuggestionProvider;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                // suggestion providers, matching CommandRunner.
                CurrentCommandContext.run(ctx, () -> {
                    // Include inherited parent arguments so argument child commands can read them while suggesting.
                    for (Arguments<C> parsed : executorArguments) {
                        try {
                            parsed.parse(ctx);
                        } catch (Exception ignored) {
                            // Best-effort: pre-populate ctx with already-parsed arguments for use in
                            // suggestion providers. Failures (e.g. platform-specific context differences)
                            // are non-fatal - the suggestion provider still runs.
                        }
                    }

                    for (ArgumentSuggestionProvider<C> suggestionProvider : argument.suggestionProviders()) {
                        SuggestionProvider<C> action = suggestionProvider.action();
                        if (action == null) {
                            defaultFutures.add(argument.type()
                                                       .listSuggestions(context, sb)
                                                       .toCompletableFuture());
                        } else {
                            action.accept(suggestionBuilder);
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

                // Vanilla parsers suggest through builders of their own (createOffset), so their results are merged
                // with what the providers added to sb.
                return customFuture.thenCombine(defaultFuture, (ignored, suggestions) -> {
                    List<Suggestions> all = new ArrayList<>();
                    all.add(sb.build());
                    defaultFutures.forEach(future -> all.add(future.join()));
                    return Suggestions.merge(sb.getInput(), all);
                });
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        });

        builder.executes(runner(argument, helpAction, parent));

        return builder;
    }

    /**
     * The runner for input that ends at the given argument of the branch.
     */
    CommandRunner<S, C> runner(CommonArgument<?, C, ?> argument,
                               CommandExecutor<C> helpAction,
                               CommonCommand<C, ?> parent) {
        return new CommandRunner<>(platformAdapter,
                                   parent,
                                   permissionPrefix,
                                   executorArguments,
                                   parent.options(),
                                   parent.prerequisite(),
                                   helpAction,
                                   parent.preprocess(),
                                   arguments.executorFor(argument),
                                   uncaughtExceptionHandlers(parent, argument));
    }

    private List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers(CommonCommand<C, ?> parent,
                                                                        CommonArgument<?, C, ?> argument) {
        List<UncaughtExceptionHandler<C>> handlers = new ArrayList<>(parent.uncaughtExceptionHandlers());
        handlers.addAll(argument.uncaughtExceptionHandlers());
        return handlers;
    }

    private List<ArgumentCommandNode<S, ?>> toCommandNodes(CommandExecutor<C> helpAction, CommonCommand<C, ?> parent) {
        // A variadic argument takes one node per value it accepts.
        return arguments.stream()
                        .flatMap(x -> IntStream.range(0, x.nodeCount())
                                               .mapToObj(i -> buildArgument(x, helpAction, parent).build()))
                        .collect(Collectors.toList());
    }

    ArgumentCommandNode<S, ?> build(CommandExecutor<C> helpAction,
                                    CommonCommand<C, ?> parent,
                                    Collection<LiteralCommandNode<S>> terminalChildren) {
        // CommonCommand rejects an argument branch without arguments, so there is always a first node.
        List<ArgumentCommandNode<S, ?>> nodes = toCommandNodes(helpAction, parent);

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
