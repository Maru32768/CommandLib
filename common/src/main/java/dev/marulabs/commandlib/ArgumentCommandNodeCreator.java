package dev.marulabs.commandlib;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.suggestion.SuggestionBuilder;
import dev.marulabs.commandlib.suggestion.SuggestionProvider;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        // Brigadier also keeps the suggestions of one node, so the node suggests what each branch's argument does, to
        // the senders of that branch.
        List<String> names = names(arguments);
        int index = names.indexOf(argument.name());
        List<String> prefix = names.subList(0, index + 1);
        List<Arguments<C>> sharing = new ArrayList<>();
        sharing.add(arguments);
        for (Arguments<C> branch : branches) {
            List<String> branchNames = names(branch);
            if (branch != arguments && branchNames.size() >= prefix.size() && branchNames.subList(0, prefix.size())
                                                                                         .equals(prefix)) {
                sharing.add(branch);
            }
        }
        List<com.mojang.brigadier.suggestion.SuggestionProvider<S>> suggestions = new ArrayList<>();
        Set<CommonArgument<?, C, ?>> suggesting = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Arguments<C> branch : sharing) {
            CommonArgument<?, C, ?> other = argumentAt(branch, index);
            if (suggesting.add(other)) {
                // Branches may reuse one argument instance, which then suggests to the senders of each of them.
                List<Arguments<C>> reusing = sharing.stream()
                                                    .filter(b -> argumentAt(b, index) == other)
                                                    .collect(Collectors.toList());
                List<Arguments<C>> otherArguments = new ArrayList<>(executorArguments.subList(0,
                                                                                              executorArguments.size()
                                                                                                      - 1));
                otherArguments.add(branch);
                suggestions.add(suggestions(platformAdapter,
                                            other,
                                            anyRequirement(reusing),
                                            parent.providers(),
                                            otherArguments));
            }
        }
        Predicate<S> requirement = anyRequirement(sharing);
        builder.requires(requirement);

        builder.suggests(suggestions.size() == 1 ? suggestions.get(0) : merged(suggestions));

        builder.executes(runner(argument, helpAction, parent));

        return builder;
    }

    private static <S> com.mojang.brigadier.suggestion.SuggestionProvider<S> merged(List<com.mojang.brigadier.suggestion.SuggestionProvider<S>> providers) {
        return (context, sb) -> {
            List<CompletableFuture<Suggestions>> futures = new ArrayList<>();
            for (com.mojang.brigadier.suggestion.SuggestionProvider<S> provider : providers) {
                futures.add(provider.getSuggestions(context, new SuggestionsBuilder(sb.getInput(), sb.getStart()))
                                    .toCompletableFuture());
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                                    .thenApply(x -> {
                                        // Suggestions.merge compares tooltips too, and a String tooltip becomes a
                                        // LiteralMessage without equals, so identical branches would list it twice.
                                        Map<List<Object>, Suggestion> unique = new LinkedHashMap<>();
                                        for (CompletableFuture<Suggestions> future : futures) {
                                            for (Suggestion suggestion : future.join()
                                                                               .getList()) {
                                                unique.putIfAbsent(List.of(suggestion.getRange(), suggestion.getText()),
                                                                   suggestion);
                                            }
                                        }
                                        return Suggestions.create(sb.getInput(), unique.values());
                                    });
        };
    }

    private static <C extends CommonCommandContext<?, ?>> CommonArgument<?, C, ?> argumentAt(Arguments<C> branch,
                                                                                             int index) {
        return branch.stream()
                     .skip(index)
                     .findFirst()
                     .orElseThrow();
    }

    private Predicate<S> anyRequirement(List<Arguments<C>> branches) {
        return branches.stream()
                       .map(this::branchRequirement)
                       .reduce(Predicate::or)
                       .orElseThrow();
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
                        // A failing provider leaves out its own suggestions only, as when its stage fails below.
                        try {
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
                        } catch (Exception e) {
                            reportSuggestionFailure(e);
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
            } catch (Exception e) {
                // Brigadier would drop the suggestions of every other node.
                reportSuggestionFailure(e);
                return Suggestions.empty();
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
