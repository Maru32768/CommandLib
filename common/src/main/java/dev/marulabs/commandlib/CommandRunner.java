package dev.marulabs.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.command.CommandExecutor;
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
import java.util.function.Predicate;

final class CommandRunner<S, C extends CommonCommandContext<S, ?>> implements Command<S> {
    private final PlatformAdapter<S, ?, C, ?> platformAdapter;
    private final CommonCommand<C, ?> command;
    private final String permissionPrefix;
    private final List<Arguments<C>> argumentsList;
    private final List<CommandOption<?, C>> options;
    private final Prerequisite<C> prerequisite;
    private final CommandExecutor<C> helpAction;
    private final Predicate<C> preprocess;
    // Null when the command or branch has no executor; the help action runs instead, without the preprocess.
    private final @Nullable CommandExecutor<C> executor;
    private final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers;

    CommandRunner(PlatformAdapter<S, ?, C, ?> platformAdapter,
                  CommonCommand<C, ?> command,
                  String permissionPrefix,
                  List<Arguments<C>> argumentsList,
                  List<CommandOption<?, C>> options,
                  Prerequisite<C> prerequisite,
                  CommandExecutor<C> helpAction,
                  Predicate<C> preprocess,
                  @Nullable CommandExecutor<C> executor,
                  List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers) {
        this.platformAdapter = platformAdapter;
        this.command = command;
        this.permissionPrefix = permissionPrefix;
        this.argumentsList = List.copyOf(argumentsList);
        this.options = options;
        this.prerequisite = prerequisite;
        this.helpAction = helpAction;
        this.preprocess = preprocess;
        this.executor = executor;
        this.uncaughtExceptionHandlers = uncaughtExceptionHandlers;
    }

    @Override
    public int run(CommandContext<S> context) throws CommandFailure {
        try {
            C ctx = platformAdapter.createCommandContext(context);
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
                return fail(ctx, () -> ctx.sendFailure("You do not have permission to execute this command."));
            }

            try {
                parseOptions(ctx);
                validateOptions(ctx);
            } catch (OptionParseException e) {
                return fail(ctx, () -> e.sendMessage(ctx));
            }

            for (Arguments<C> arguments : argumentsList) {
                if (!platformAdapter.hasPermission(ctx, arguments.permissionName(permissionPrefix))) {
                    return fail(ctx, () -> ctx.sendFailure("You do not have permission to execute this command."));
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
                return executeWithStackTrace(ctx, helpAction);
            }

            // The preprocess sends its messages before returning, so they are collected until it tells whether the
            // command fails.
            boolean[] passed = new boolean[1];
            CommandFailure preprocessFailure = ctx.captureFailure(() -> passed[0] = preprocess.test(ctx));
            if (!passed[0]) {
                return fail(preprocessFailure);
            }
            preprocessFailure.sendMessages();

            return executeWithStackTrace(ctx, executor);
        } catch (CommandFailure e) {
            throw e;
        } catch (Throwable e) {
            // Exceptions escaping parsing, prerequisites or preprocess are not reported to the sender, so the handlers
            // get them before Brigadier propagates them to the platform.
            e.printStackTrace();
            notifyUncaughtExceptionHandlers(e, ctx);
            throw e;
        }
    }

    private int executeWithStackTrace(C ctx, CommandExecutor<C> executor) throws CommandFailure {
        try {
            executor.accept(ctx);
            return ctx.getResult();
        } catch (CommandPrerequisiteException e) {
            return fail(ctx, () -> e.sendMessage(ctx));
        } catch (Exception e) {
            e.printStackTrace();
            CommandFailure failure = ctx.captureFailure(() -> {
                ctx.sendFailure("An unexpected error occurred trying to execute that command.");
                ctx.sendFailure("Check the console for details.");
            });
            notifyUncaughtExceptionHandlers(e, ctx);
            return fail(failure);
        }
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

    // common has no logger, so a stack trace on the console is how it reports, as in run and executeWithStackTrace.
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
