package dev.marulabs.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.CommandMiddleware;
import dev.marulabs.commandlib.command.Prerequisite;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.exception.OptionParseException;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final PlatformAdapter<S, ?, C, ?> platformAdapter;
    private final CommandScheduler scheduler;
    private final CommonCommand<C, ?> command;
    private final String permissionPrefix;
    private final List<Arguments<C>> argumentsList;
    private final List<CommandOption<?, C>> options;
    private final Prerequisite<C> prerequisite;
    private final CommandExecutor<C> helpAction;
    private final List<CommandMiddleware<? super C>> middlewares;
    // Null when the command or branch has no executor; the help action runs instead, without the middlewares.
    private final @Nullable AsyncCommandExecutor<C> executor;
    private final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers;

    CommandRunner(PlatformAdapter<S, ?, C, ?> platformAdapter,
                  CommandScheduler scheduler,
                  CommonCommand<C, ?> command,
                  String permissionPrefix,
                  List<Arguments<C>> argumentsList,
                  List<CommandOption<?, C>> options,
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
        this.options = options;
        this.prerequisite = prerequisite;
        this.helpAction = helpAction;
        this.middlewares = List.copyOf(middlewares);
        this.executor = executor;
        this.uncaughtExceptionHandlers = uncaughtExceptionHandlers;
    }

    @Override
    public int run(CommandContext<S> context) throws CommandFailure {
        try {
            C ctx = platformAdapter.createCommandContext(context);
            ctx.scheduler(scheduler);
            ctx.captions(command.captions());
            return CurrentCommandContext.call(ctx, () -> runInContext(ctx));
        } catch (CommandFailure e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    private int runInContext(C ctx) throws CommandFailure {
        try {
            if (!platformAdapter.hasPermission(ctx, command.permissionName(permissionPrefix))) {
                return fail(ctx, () -> ctx.sendFailure(Caption.PERMISSION_DENIED));
            }

            try {
                parseOptions(ctx);
                validateOptions(ctx);
            } catch (OptionParseException e) {
                return fail(ctx, () -> e.sendMessage(ctx));
            }

            for (Arguments<C> arguments : argumentsList) {
                if (!platformAdapter.hasPermission(ctx, arguments.permissionName(permissionPrefix))) {
                    return fail(ctx, () -> ctx.sendFailure(Caption.PERMISSION_DENIED));
                }

                try {
                    arguments.parse(ctx);
                } catch (ArgumentParseException e) {
                    return fail(ctx, () -> e.sendMessage(ctx));
                } catch (CommandPrerequisiteException e) {
                    // The default value of an omitted optional argument can need a prerequisite.
                    return fail(ctx, () -> e.sendMessage(ctx));
                }
            }
            try {
                prerequisite.check(ctx);
            } catch (CommandPrerequisiteException e) {
                return fail(ctx, () -> e.sendMessage(ctx));
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
            // before Brigadier propagates them to the platform.
            e.printStackTrace();
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

    // common has no logger, so a stack trace on the console is how it reports, as in run.
    @SuppressWarnings("CatchAndPrintStackTrace")
    private void reportLateFailure(C ctx, Throwable e) {
        try {
            // The dispatch has returned, so the messages go to the sender directly.
            CurrentCommandContext.run(ctx, () -> toFailure(ctx, e).sendMessages());
        } catch (RuntimeException t) {
            t.printStackTrace();
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
            return ctx.captureFailure(() -> ((CommandPrerequisiteException) e).sendMessage(ctx));
        }
        if (e instanceof CancellationException) {
            // Someone cancelled the work on purpose, so there is nothing to report.
            return ctx.captureFailure(() -> {
            });
        }
        if (e instanceof TimeoutException) {
            CommandFailure failure = ctx.captureFailure(() -> ctx.sendFailure(Caption.TIMEOUT));
            notifyUncaughtExceptionHandlers(e, ctx);
            return failure;
        }
        e.printStackTrace();
        CommandFailure failure = ctx.captureFailure(() -> {
            ctx.sendFailure(Caption.UNEXPECTED_ERROR);
            ctx.sendFailure(Caption.SEE_CONSOLE);
        });
        notifyUncaughtExceptionHandlers(e, ctx);
        return failure;
    }

    private static int fail(CommonCommandContext<?, ?> ctx, Runnable sendMessages) throws CommandFailure {
        return fail(ctx.captureFailure(sendMessages));
    }

    private static int fail(CommandFailure failure) throws CommandFailure {
        // The platform would send an empty line for a failure without a message, so such a failure only returns 0.
        if (!failure.hasMessages()) {
            return 0;
        }
        throw failure;
    }

    // common has no logger, so a stack trace on the console is how it reports, as in run.
    @SuppressWarnings("CatchAndPrintStackTrace")
    private void notifyUncaughtExceptionHandlers(Throwable e, C ctx) {
        // A failing handler must neither keep the remaining handlers from running nor escape to the outer catch,
        // which would notify every handler again with the handler's own exception.
        for (UncaughtExceptionHandler<C> handler : uncaughtExceptionHandlers) {
            try {
                handler.uncaughtException(e, ctx);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void parseOptions(C ctx) {
        Map<String, CommandOption<?, C>> flagByLongToken = new HashMap<>();
        Map<Character, CommandOption<?, C>> flagByShortName = new HashMap<>();
        Map<String, CommandOption<?, C>> valueOptionByInternalName = new HashMap<>();

        for (CommandOption<?, C> option : options) {
            if (option.hasValue()) {
                for (int i = 0; i < option.maxOccurrences(); i++) {
                    valueOptionByInternalName.put(option.internalName(i), option);
                }
                continue;
            }
            option.names()
                  .forEach(x -> flagByLongToken.put("--" + x, option));
            if (option.shortName() != null) {
                flagByShortName.put(option.shortName(), option);
            }
        }

        // The node names of each option's occurrences in input order. A flag has its token once per occurrence.
        Map<CommandOption<?, C>, List<String>> occurrences = new LinkedHashMap<>();
        ctx.getHandle()
           .getNodes()
           .stream()
           .map(x -> x.getNode()
                      .getName())
           .forEach(token -> {
               CommandOption<?, C> valueOption = valueOptionByInternalName.get(token);
               if (valueOption != null) {
                   occurrences.computeIfAbsent(valueOption, x -> new ArrayList<>())
                              .add(token);
                   return;
               }
               if (!token.startsWith("-")) {
                   return;
               }

               CommandOption<?, C> longFlag = flagByLongToken.get(token);
               if (longFlag != null) {
                   occurrences.computeIfAbsent(longFlag, x -> new ArrayList<>())
                              .add(token);
                   return;
               }
               if (token.startsWith("--")) {
                   return;
               }

               for (int i = 1; i < token.length(); i++) {
                   CommandOption<?, C> flag = flagByShortName.get(token.charAt(i));
                   if (flag != null) {
                       occurrences.computeIfAbsent(flag, x -> new ArrayList<>())
                                  .add(token);
                   }
               }
           });

        occurrences.forEach((option, names) -> ctx.setOptionValue(option, option.read(ctx, names)));
    }

    private void validateOptions(C ctx) throws OptionParseException {
        // Help runs where there is no executor, and must not ask for the options the executor requires.
        for (CommandOption<?, C> option : options) {
            option.validate(ctx, executor != null);
        }
    }
}
