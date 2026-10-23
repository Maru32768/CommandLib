package dev.marulabs.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.CommandMiddleware;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.command.Prerequisite;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.exception.ArgumentValidationException;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.exception.OptionParseException;
import dev.marulabs.commandlib.internal.CurrentCommandContext;
import dev.marulabs.commandlib.internal.PlatformAdapter;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

final class CommandRunner<S, C extends CommonCommandContext<S, ?>> implements Command<S> {
    private final PlatformAdapter<S, ?, C> platformAdapter;
    private final CommandScheduler scheduler;
    private final CommonCommand<C, ?> command;
    private final String permissionPrefix;
    private final List<Arguments<C>> argumentsList;
    private final OptionGraph<S, C> optionGraph;
    private final Prerequisite<C> prerequisite;
    private final CommandExecutor<C> helpAction;
    private final List<CommandMiddleware<? super C>> middlewares;
    // Null when the command or branch has no executor; the help action runs instead, without the middlewares.
    private final @Nullable AsyncCommandExecutor<C> executor;
    private final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers;
    private final Map<ContextKey<?>, Extractor<? super C, ?>> providers;

    CommandRunner(PlatformAdapter<S, ?, C> platformAdapter,
                  CommandScheduler scheduler,
                  CommonCommand<C, ?> command,
                  String permissionPrefix,
                  List<Arguments<C>> argumentsList,
                  OptionGraph<S, C> optionGraph,
                  Prerequisite<C> prerequisite,
                  CommandExecutor<C> helpAction,
                  List<CommandMiddleware<? super C>> middlewares,
                  @Nullable AsyncCommandExecutor<C> executor,
                  List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers) {
        this.platformAdapter = platformAdapter;
        this.scheduler = scheduler;
        this.command = command;
        this.permissionPrefix = permissionPrefix;
        this.argumentsList = List.copyOf(argumentsList);
        this.optionGraph = optionGraph;
        this.prerequisite = prerequisite;
        this.helpAction = helpAction;
        this.middlewares = List.copyOf(middlewares);
        this.executor = executor;
        this.uncaughtExceptionHandlers = uncaughtExceptionHandlers;
        this.providers = command.providers();
    }

    @Override
    public int run(CommandContext<S> context) throws CommandSyntaxException {
        C ctx;
        try {
            // After options, Brigadier executes a context without them; the graph adds them back.
            ctx = platformAdapter.createCommandContext(optionGraph.recover(context));
            // Brigadier gives the targets of a forked execution copies of one context, which share its node list.
            ctx.parse(context.getNodes());
            ctx.scheduler(scheduler);
            ctx.captions(command.captions());
            ctx.providers(providers);
            checkVisible(ctx);
        } catch (CommandSyntaxException e) {
            throw e;
        } catch (Throwable e) {
            platformAdapter.logError("Could not set up the execution of /" + context.getInput(), e);
            throw e;
        }
        // runInContext logs and reports what it does not send to the sender itself.
        return CurrentCommandContext.call(ctx, () -> runInContext(ctx));
    }

    // A command or branch hidden from the sender (visibleWhen) is rejected as Brigadier rejects a node the sender
    // cannot use, for a state that changed after the input was parsed or a platform that runs it anyway.
    private void checkVisible(C ctx) throws CommandSyntaxException {
        for (CommonCommand<C, ?> current = command; current != null; current = current.parent()) {
            if (!current.isVisible(ctx)) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand()
                                                                .create();
            }
        }
        for (Arguments<C> arguments : argumentsList) {
            if (!arguments.isVisible(ctx)) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()
                                                                .create();
            }
        }
    }

    private int runInContext(C ctx) throws CommandFailure {
        try {
            // The parse may be older than the sender's permissions, as a function's is, so every node of the route that
            // the command tree requires is checked again: the parent commands here, the branches and options below.
            for (CommonCommand<C, ?> current = command; current != null; current = current.parent()) {
                if (!platformAdapter.hasPermission(ctx, current.permissionName(permissionPrefix))) {
                    return denied(ctx);
                }
            }

            try {
                parseOptions(ctx);
                for (CommandOption<?, C> option : optionGraph.options()) {
                    String permission = optionGraph.permissionName(option);
                    if (permission != null && ctx.hasOption(option)
                            && !platformAdapter.hasPermission(ctx, permission)) {
                        return denied(ctx);
                    }
                }
                validateOptions(ctx);
            } catch (OptionParseException e) {
                return fail(ctx, CommandError.Kind.OPTION, e, null, () -> e.sendMessage(ctx));
            } catch (ArgumentParseException e) {
                // The value of an option of an argument failed the argument's parsing or validation.
                return fail(ctx,
                            e instanceof ArgumentValidationException ? CommandError.Kind.VALIDATION
                                    : CommandError.Kind.ARGUMENT,
                            e,
                            null,
                            () -> e.sendMessage(ctx));
            } catch (CommandPrerequisiteException e) {
                // The default of an option of an optional argument can need a prerequisite.
                return fail(ctx, CommandError.Kind.PREREQUISITE, e, null, () -> e.sendMessage(ctx));
            }

            for (Arguments<C> arguments : argumentsList) {
                if (!platformAdapter.hasPermission(ctx, arguments.permissionName(permissionPrefix))) {
                    return denied(ctx);
                }

                try {
                    arguments.parse(ctx);
                } catch (ArgumentParseException e) {
                    // The owner is a parent of this command for an inherited branch.
                    boolean options = !HelpMessageAction.usableOptions(platformAdapter,
                                                                       arguments.owner(),
                                                                       permissionPrefix,
                                                                       ctx)
                                                        .isEmpty();
                    return fail(ctx,
                                e instanceof ArgumentValidationException ? CommandError.Kind.VALIDATION
                                        : CommandError.Kind.ARGUMENT,
                                e,
                                HelpMessageAction.branchUsage(arguments, ctx, options),
                                () -> e.sendMessage(ctx));
                } catch (CommandPrerequisiteException e) {
                    // The default value of an omitted optional argument can need a prerequisite.
                    return fail(ctx, CommandError.Kind.PREREQUISITE, e, null, () -> e.sendMessage(ctx));
                }
            }
            try {
                prerequisite.check(ctx);
            } catch (CommandPrerequisiteException e) {
                return fail(ctx, CommandError.Kind.PREREQUISITE, e, null, () -> e.sendMessage(ctx));
            }

            if (executor == null) {
                try {
                    helpAction.accept(ctx);
                    return ctx.getResult();
                } catch (Exception e) {
                    return fail(toFailure(ctx, e));
                }
            }

            CompletableFuture<Void> completion = proceed(ctx, 0, executor);
            if (completion.isDone()) {
                Throwable failure = failureOf(completion);
                return failure == null ? ctx.getResult() : fail(toFailure(ctx, failure));
            }
            // Brigadier needs the result now, so an execution still running returns the result set so far, 1 unless
            // set, and reports its failure to the sender when it completes.
            int result = ctx.getResult();
            whenComplete(completion, ctx);
            return result;
        } catch (CommandFailure e) {
            throw e;
        } catch (Throwable e) {
            // Exceptions escaping parsing or prerequisites are not reported to the sender, so the handlers get them
            // before Brigadier propagates them to the platform, which does not log them.
            logUnexpected(ctx, e);
            notifyUncaughtExceptionHandlers(e, ctx);
            throw e;
        }
    }

    /**
     * Runs the middleware at {@code index}, or the executor after the last one, on the sync thread, inline when the
     * caller is on it.
     */
    // The nested future is flattened by thenCompose, not ignored.
    @SuppressWarnings("FutureReturnValueIgnored")
    private CompletableFuture<Void> proceed(C ctx, int index, AsyncCommandExecutor<C> executor) {
        return CompletableFuture.supplyAsync(() -> CurrentCommandContext.call(ctx, () -> step(ctx, index, executor)),
                                             scheduler.sync(ctx))
                                .thenCompose(Function.identity());
    }

    private CompletableFuture<Void> step(C ctx, int index, AsyncCommandExecutor<C> executor) {
        CompletableFuture<?> stage;
        try {
            CompletionStage<?> result;
            if (index < middlewares.size()) {
                result = middlewares.get(index)
                                    .handle(ctx, () -> proceed(ctx, index + 1, executor));
            } else {
                result = executor.apply(ctx);
            }
            stage = Objects.requireNonNull(result, "A middleware or async executor returned null")
                           .toCompletableFuture();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
        if (stage.isDone()) {
            return stage.thenApply(x -> null);
        }
        // Brings the completion back to the sync thread, so the outer middlewares' hooks run there.
        return stage.whenCompleteAsync((x, e) -> {
        }, scheduler.sync(ctx))
                    .thenApply(x -> null);
    }

    // The completion runs the reporting; its own future has nothing more to report.
    @SuppressWarnings("FutureReturnValueIgnored")
    private void whenComplete(CompletableFuture<Void> completion, C ctx) {
        completion.whenCompleteAsync((ignored, e) -> {
            if (e != null) {
                reportLateFailure(ctx, unwrap(e));
            }
        }, scheduler.sync(ctx));
    }

    private void reportLateFailure(C ctx, Throwable e) {
        try {
            // The dispatch has returned, so the messages go to the sender directly.
            CurrentCommandContext.run(ctx, () -> toFailure(ctx, e).sendMessages());
        } catch (RuntimeException t) {
            platformAdapter.logError("Could not report the failure of " + describe(ctx), t);
        }
    }

    @Nullable
    private static Throwable failureOf(CompletableFuture<?> completion) {
        try {
            completion.join();
            return null;
        } catch (CancellationException e) {
            return e;
        } catch (CompletionException e) {
            return unwrap(e);
        }
    }

    private static Throwable unwrap(Throwable e) {
        Throwable t = e;
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /**
     * The failure that reports {@code e} from a middleware, the executor or the help action, with its messages
     * collected. An unexpected exception is printed and passed to the uncaught exception handlers.
     */
    private CommandFailure toFailure(C ctx, Throwable e) {
        if (e instanceof CommandFailure) {
            return (CommandFailure) e;
        }
        if (e instanceof CommandPrerequisiteException) {
            return capture(ctx,
                           CommandError.Kind.PREREQUISITE,
                           e,
                           null,
                           () -> ((CommandPrerequisiteException) e).sendMessage(ctx));
        }
        if (e instanceof CancellationException) {
            // Someone canceled the work on purpose, so there is nothing to report.
            return ctx.captureFailure(() -> {
            });
        }
        if (e instanceof TimeoutException) {
            notifyUncaughtExceptionHandlers(e, ctx);
            return capture(ctx, CommandError.Kind.TIMEOUT, e, null, () -> ctx.sendFailure(Caption.TIMEOUT));
        }
        logUnexpected(ctx, e);
        notifyUncaughtExceptionHandlers(e, ctx);
        return capture(ctx, CommandError.Kind.UNEXPECTED, e, null, () -> {
            ctx.sendFailure(Caption.UNEXPECTED_ERROR);
            ctx.sendFailure(Caption.SEE_CONSOLE);
        }).unexpected(e);
    }

    private int denied(C ctx) throws CommandFailure {
        return fail(ctx,
                    CommandError.Kind.PERMISSION_DENIED,
                    null,
                    null,
                    () -> ctx.sendFailure(Caption.PERMISSION_DENIED));
    }

    private int fail(C ctx,
                     CommandError.Kind kind,
                     @Nullable Throwable exception,
                     CommandHelp.@Nullable Usage usage,
                     Runnable sendDefault) throws CommandFailure {
        return fail(capture(ctx, kind, exception, usage, sendDefault));
    }

    /**
     * Collects the messages the command's {@link ErrorFormatter} sends for the failure.
     */
    private CommandFailure capture(C ctx,
                                   CommandError.Kind kind,
                                   @Nullable Throwable exception,
                                   CommandHelp.@Nullable Usage usage,
                                   Runnable sendDefault) {
        CommandError error = new CommandError(kind, exception, usage, sendDefault);
        return ctx.captureFailure(() -> {
            try {
                command.errorFormatter()
                       .format(ctx, error);
            } catch (RuntimeException e) {
                // A broken formatter must not hide the failure it reports.
                platformAdapter.logError("The error formatter of " + describe(ctx) + " failed", e);
                sendDefault.run();
            }
        });
    }

    private static int fail(CommandFailure failure) throws CommandFailure {
        // The platform would send an empty line for a failure without a message, so such a failure only returns 0,
        // unless it carries an AssertionError that a tester dispatching it must rethrow.
        if (!failure.hasMessages() && !(failure.hasAssertionError() && CommandFailure.isReplayed())) {
            return 0;
        }
        throw failure;
    }

    private void notifyUncaughtExceptionHandlers(Throwable e, C ctx) {
        // A failing handler must neither keep the remaining handlers from running nor escape to the outer catch,
        // which would notify every handler again with the handler's own exception.
        for (UncaughtExceptionHandler<C> handler : uncaughtExceptionHandlers) {
            try {
                handler.uncaughtException(e, ctx);
            } catch (Throwable t) {
                platformAdapter.logError("An uncaught exception handler of " + describe(ctx) + " failed", t);
            }
        }
    }

    private void logUnexpected(C ctx, Throwable e) {
        platformAdapter.logError("Unexpected exception while executing " + describe(ctx), e);
    }

    // The input with the sender, such as "/ban Steve by Alex".
    private static String describe(CommonCommandContext<?, ?> ctx) {
        return "/" + ctx.getHandle()
                        .getInput()
                + " by " + ctx.getActor()
                              .getName();
    }

    private void parseOptions(C ctx) throws ArgumentParseException, CommandPrerequisiteException {
        Map<CommandOption<?, C>, List<String>> occurrences = optionGraph.occurrences(ctx.getHandle());
        for (Map.Entry<CommandOption<?, C>, List<String>> entry : occurrences.entrySet()) {
            ctx.setOptionValue(entry.getKey(),
                               entry.getKey()
                                    .read(ctx, entry.getValue()));
        }
        if (executor == null) {
            // Help reads no option, and the default of an argument may need a prerequisite help does not.
            return;
        }
        for (CommandOption<?, C> option : optionGraph.options()) {
            Object value = occurrences.containsKey(option) ? null : option.defaultValue(ctx);
            if (value != null) {
                ctx.setOptionDefault(option, value);
            }
        }
    }

    private void validateOptions(C ctx) throws OptionParseException {
        // Help runs where there is no executor, and must not ask for the options the executor requires.
        for (CommandOption<?, C> option : optionGraph.options()) {
            option.validate(ctx, executor != null);
        }
    }
}
