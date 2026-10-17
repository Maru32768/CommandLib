package dev.marulabs.commandlib;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.suggestion.SuggestionBuilder;
import dev.marulabs.commandlib.suggestion.SuggestionProvider;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class ArgumentCommandNodeCreator<S, T, C extends CommonCommandContext<S, T>> {
    private final PlatformAdapter<S, T, C, ?> platformAdapter = PlatformAdapter.get();
    private final Arguments<C> arguments;
    // The branches of the command. One shares the node of an argument with this branch when the names of the arguments
    // up to it are the same.
    private final List<Arguments<C>> branches;
    private final List<Arguments<C>> executorArguments;
    private final OptionGraph<S, C> optionGraph;
    private final String permissionPrefix;
    private final CommandScheduler scheduler;

    ArgumentCommandNodeCreator(Arguments<C> arguments,
                               List<Arguments<C>> branches,
                               List<Arguments<C>> executorArguments,
                               OptionGraph<S, C> optionGraph,
                               String permissionPrefix,
                               CommandScheduler scheduler) {
        this.scheduler = scheduler;
        this.arguments = arguments;
        this.branches = List.copyOf(branches);
        this.executorArguments = List.copyOf(executorArguments);
        this.optionGraph = optionGraph;
        this.permissionPrefix = permissionPrefix;
    }

    private RequiredArgumentBuilder<S, ?> buildArgument(CommonArgument<?, C, ?> argument,
                                                        CommandExecutor<C> helpAction,
                                                        CommonCommand<C, ?> parent) {
        RequiredArgumentBuilder<S, ?> builder = RequiredArgumentBuilder.argument(argument.name(), argument.type());
        // Brigadier merges the nodes of the branches that share the argument and keeps one requirement, so it lets
        // through the sender of any of them; the runner checks the branch it executes.
        List<String> names = names(arguments);
        List<String> prefix = names.subList(0, names.indexOf(argument.name()) + 1);
        Predicate<S> requirement = branchRequirement(arguments);
        for (Arguments<C> branch : branches) {
            List<String> branchNames = names(branch);
            if (branch != arguments && branchNames.size() >= prefix.size() && branchNames.subList(0, prefix.size())
                                                                                         .equals(prefix)) {
                requirement = requirement.or(branchRequirement(branch));
            }
        }
        builder.requires(requirement);

        builder.suggests(suggestions(platformAdapter, argument, requirement, parent.providers(), executorArguments));

        builder.executes(runner(argument, helpAction, parent));

        return builder;
    }

    // The sender has the branch's permission and the branch is visible to the sender.
    private Predicate<S> branchRequirement(Arguments<C> branch) {
        String permission = branch.permissionName(permissionPrefix);
        return branch.hasVisibility()
                ? source -> platformAdapter.hasPermission(source, permission)
                        && branch.isVisibleTo(platformAdapter.createSourceContext(source))
                : source -> platformAdapter.hasPermission(source, permission);
    }

    private static List<String> names(Arguments<?> branch) {
        return branch.stream()
                     .map(CommonArgument::name)
                     .collect(Collectors.toList());
    }

    /**
     * The suggestions of an argument node: the argument's suggestion providers, with the arguments parsed before it.
     * They are empty for a source that fails {@code requirement}.
     */
    static <S, C extends CommonCommandContext<S, ?>> com.mojang.brigadier.suggestion.SuggestionProvider<S> suggestions(PlatformAdapter<S, ?, C, ?> platformAdapter,
                                                                                                                       CommonArgument<?, C, ?> argument,
                                                                                                                       Predicate<S> requirement,
                                                                                                                       Map<ContextKey<?>, ? extends Extractor<?, ?>> providers,
                                                                                                                       List<Arguments<C>> executorArguments) {
        return (context, sb) -> {
            try {
                if (!requirement.test(context.getSource())) {
                    return Suggestions.empty();
                }
                // Brigadier suggests with the first context of the input, which has the nodes before the first
                // redirect only, such as those of "execute run" or of a Paper label that redirects to
                // <plugin>:<label>.
                C ctx = platformAdapter.createCommandContext(ChainedCommandContext.flatten(context));
                ctx.providers(providers);
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
                                                       .toCompletableFuture()
                                                       .exceptionally(e -> {
                                                           reportSuggestionFailure(e);
                                                           return Suggestions.empty()
                                                                             .join();
                                                       }));
                        } else {
                            action.accept(suggestionBuilder);
                        }
                    }
                });

                // Brigadier's getCompletionSuggestions never completes when one node's future fails, so a failed
                // stage keeps the suggestions added so far.
                CompletableFuture<Void> customFuture = suggestionBuilder.awaitAll()
                                                                        .toCompletableFuture()
                                                                        .handle((ignored, e) -> {
                                                                            if (e != null) {
                                                                                reportSuggestionFailure(e);
                                                                            }
                                                                            suggestionBuilder.build()
                                                                                             .forEach(s -> s.suggest(sb));
                                                                            return null;
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
        };
    }

    // common has no logger, so a stack trace on the console is how it reports, as CommandRunner does.
    @SuppressWarnings("CatchAndPrintStackTrace")
    private static void reportSuggestionFailure(Throwable e) {
        (e instanceof CompletionException && e.getCause() != null ? e.getCause() : e).printStackTrace();
    }

    /**
     * The runner for input that ends at the given argument of the branch.
     */
    CommandRunner<S, C> runner(CommonArgument<?, C, ?> argument,
                               CommandExecutor<C> helpAction,
                               CommonCommand<C, ?> parent) {
        // Brigadier merges the nodes of the branches that share the argument and keeps the command of the node added
        // last, so every branch gives the node the runner of the branch that executes there, if any.
        Arguments<C> branch = arguments;
        CommonArgument<?, C, ?> branchArgument = argument;
        if (arguments.executorFor(argument) == null) {
            List<String> names = names(arguments);
            int index = names.indexOf(argument.name());
            List<String> prefix = names.subList(0, index + 1);
            for (Arguments<C> other : branches) {
                List<String> otherNames = names(other);
                if (otherNames.size() > index && otherNames.subList(0, index + 1)
                                                           .equals(prefix)) {
                    CommonArgument<?, C, ?> otherArgument = other.stream()
                                                                 .skip(index)
                                                                 .findFirst()
                                                                 .orElseThrow();
                    if (other.executorFor(otherArgument) != null) {
                        branch = other;
                        branchArgument = otherArgument;
                        break;
                    }
                }
            }
        }
        return runner(branch, branchArgument, helpAction, parent);
    }

    /**
     * The runner for input that stops before the branch's arguments, when they are all optional. The input reaches no
     * argument node, so it runs this branch's own executor or help, never that of a branch sharing the first node.
     */
    CommandRunner<S, C> bareRunner(CommandExecutor<C> helpAction, CommonCommand<C, ?> parent) {
        return runner(arguments, arguments.first(), helpAction, parent);
    }

    private CommandRunner<S, C> runner(Arguments<C> branch,
                                       CommonArgument<?, C, ?> branchArgument,
                                       CommandExecutor<C> helpAction,
                                       CommonCommand<C, ?> parent) {
        List<Arguments<C>> branchArguments = new ArrayList<>(executorArguments.subList(0,
                                                                                       executorArguments.size() - 1));
        branchArguments.add(branch);
        return new CommandRunner<>(platformAdapter,
                                   scheduler,
                                   parent,
                                   permissionPrefix,
                                   branchArguments,
                                   optionGraph,
                                   parent.prerequisite(),
                                   helpAction,
                                   parent.middlewares(),
                                   branch.executorFor(branchArgument),
                                   uncaughtExceptionHandlers(parent, branchArgument));
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
