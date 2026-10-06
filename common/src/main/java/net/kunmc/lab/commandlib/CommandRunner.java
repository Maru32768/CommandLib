package net.kunmc.lab.commandlib;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.command.Prerequisite;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.exception.CommandPrerequisiteException;
import net.kunmc.lab.commandlib.util.UncaughtExceptionHandler;

import java.util.HashMap;
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
    private final CommandExecutor<C> executor;
    private final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers;

    CommandRunner(PlatformAdapter<S, ?, C, ?> platformAdapter,
                  CommonCommand<C, ?> command,
                  String permissionPrefix,
                  List<Arguments<C>> argumentsList,
                  List<CommandOption<?, C>> options,
                  Prerequisite<C> prerequisite,
                  CommandExecutor<C> helpAction,
                  Predicate<C> preprocess,
                  CommandExecutor<C> executor,
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
    public int run(CommandContext<S> context) {
        try {
            C ctx = platformAdapter.createCommandContext(context);
            return CurrentCommandContext.call(ctx, () -> runInContext(ctx));
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    private int runInContext(C ctx) {
        try {
            if (!platformAdapter.hasPermission(ctx, command.permissionName(permissionPrefix))) {
                ctx.sendFailure("You do not have permission to execute this command.");
                return 0;
            }

            try {
                parseOptions(ctx);
                validateOptions(ctx);
            } catch (ArgumentParseException e) {
                e.sendMessage(ctx);
                return 0;
            }

            for (Arguments<C> arguments : argumentsList) {
                if (!platformAdapter.hasPermission(ctx, arguments.permissionName(permissionPrefix))) {
                    ctx.sendFailure("You do not have permission to execute this command.");
                    return 0;
                }

                try {
                    arguments.parse(ctx);
                } catch (ArgumentParseException e) {
                    e.sendMessage(ctx);
                    return 0;
                }
            }
            try {
                prerequisite.check(ctx);
            } catch (CommandPrerequisiteException e) {
                e.sendMessage(ctx);
                return 0;
            }

            if (executor == null) {
                return executeWithStackTrace(ctx, helpAction);
            }

            if (!preprocess.test(ctx)) {
                return 0;
            }

            return executeWithStackTrace(ctx, executor);
        } catch (Throwable e) {
            // Exceptions escaping parsing, prerequisites or preprocess are not reported to the sender, so the handlers
            // get them before Brigadier propagates them to the platform.
            e.printStackTrace();
            uncaughtExceptionHandlers.forEach(x -> x.uncaughtException(e, ctx));
            throw e;
        }
    }

    private int executeWithStackTrace(C ctx, CommandExecutor<C> executor) {
        try {
            executor.accept(ctx);
            return 1;
        } catch (CommandPrerequisiteException e) {
            e.sendMessage(ctx);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            ctx.sendFailure("An unexpected error occurred trying to execute that command.");
            ctx.sendFailure("Check the console for details.");
            uncaughtExceptionHandlers.forEach(x -> x.uncaughtException(e, ctx));
            return 0;
        }
    }

    private void parseOptions(C ctx) {
        Map<String, CommandOption<?, C>> longOptionByToken = new HashMap<>();
        Map<Character, CommandOption<?, C>> shortOptionByToken = new HashMap<>();

        for (CommandOption<?, C> option : options) {
            longOptionByToken.put("--" + option.name(), option);
            if (option.shortName() != null) {
                shortOptionByToken.put(option.shortName(), option);
            }
        }

        ctx.getHandle()
           .getNodes()
           .stream()
           .map(x -> x.getNode()
                      .getName())
           .filter(x -> x.startsWith("-"))
           .forEach(token -> {
               CommandOption<?, C> longOption = longOptionByToken.get(token);
               if (longOption != null) {
                   if (!longOption.hasValue()) {
                       ctx.setOptionValue(longOption, true);
                   }
                   return;
               }

               if (token.startsWith("--")) {
                   return;
               }

               for (int i = 1; i < token.length(); i++) {
                   CommandOption<?, C> option = shortOptionByToken.get(token.charAt(i));
                   if (option != null && !option.hasValue()) {
                       ctx.setOptionValue(option, true);
                   }
               }
           });

        ctx.getHandle()
           .getNodes()
           .stream()
           .map(x -> x.getNode()
                      .getName())
           .filter(x -> x.startsWith(CommandOption.INTERNAL_NAME_PREFIX))
           .forEach(name -> options.stream()
                                   .filter(CommandOption::hasValue)
                                   .filter(x -> x.internalName()
                                                 .equals(name))
                                   .findFirst()
                                   .ifPresent(x -> ctx.setOptionValue(x, x.parse(ctx))));
    }

    private void validateOptions(C ctx) throws ArgumentParseException {
        for (CommandOption<?, C> option : options) {
            option.validate(ctx);
        }
    }
}
