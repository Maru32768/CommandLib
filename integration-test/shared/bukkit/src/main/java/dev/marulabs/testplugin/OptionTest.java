package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.CommandOption;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.Options;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OptionTest extends TestBase {
    /**
     * Sub command whose {@code --secret} option ops do not have. The bot checks that the option is rejected, since the
     * console has every permission.
     */
    public static final String OPTION_PERMISSION_PROBE = "optionPermissionProbe";
    private final String playerName;

    public OptionTest(Command command, String playerName) {
        super(command);
        this.playerName = playerName;
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.addAll(flagAndValueOptions());
        commands.addAll(defaultOptionValues());
        commands.addAll(combinedShortFlags());
        commands.addAll(requiredOption());
        commands.addAll(requiredOptionValue());
        commands.addAll(requiredOptionValueInvalid());
        commands.addAll(valueOptionWithoutValue());
        commands.addAll(optionAfterArgument());
        commands.addAll(countAndRepeatableOptions());
        commands.addAll(argumentOption());
        commands.addAll(optionsAnywhere());
        commands.addAll(aliasAndExclusiveOptions());
        commands.addAll(optionPermission());
        commands.addAll(visibleWhenRefresh());
        optionPermissionProbe();

        return commands;
    }

    public List<String> flagAndValueOptions() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                         .description("Force execution"));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100)
                                                                         .description("Maximum count"));
            CommandOption<String, CommandContext> format = option(Options.string("format", 'F', "text")
                                                                         .description("Output format"));

            argument(new StringArgument("str", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(limit) + ":" + ctx.getOption(format) + ":"
                                  + target,
                          "true:20:json:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -f -n 20 --format json alex"));
    }

    public List<String> defaultOptionValues() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            CommandOption<String, CommandContext> format = option(Options.string("format", 'F', "text"));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(limit) + ":" + ctx.getOption(format) + ":"
                                  + target,
                          "false:10:text:alex");
            });
        }});

        return List.of(buildCommand(command, name + " alex"));
    }

    public List<String> combinedShortFlags() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Boolean, CommandContext> verbose = option(Options.flag("verbose", 'v'));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(verbose) + ":" + target,
                          "true:true:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -fv alex"));
    }

    public List<String> requiredOption() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<String, CommandContext> reason = option(Options.string("reason", 'r', "")
                                                                         .requires(force));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(reason) + ":" + target,
                          "true:cleanup:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -f -r cleanup alex"));
    }

    public List<String> requiredOptionValue() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<String, CommandContext> mode = option(Options.string("mode", 'm', "normal"));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10)
                                                                         .requires(mode, "parallel"));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key, ctx.getOption(mode) + ":" + ctx.getOption(limit) + ":" + target, "parallel:20:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -n 20 -m parallel alex"));
    }

    public List<String> requiredOptionValueInvalid() {
        String name = getMethodName();
        String key = getKey();

        // The rejected input is dispatched first and a valid input second, so the result only succeeds when the
        // command exists and the rejected input did not run it.
        putResult(new TestResult(key, TestStatus.FAILED, "Valid control input was not executed."));
        AtomicBoolean rejectedInputExecuted = new AtomicBoolean();
        command.addChildren(new Command(name) {{
            CommandOption<String, CommandContext> mode = option(Options.string("mode", 'm', "normal"));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10)
                                                                         .requires(mode, "parallel"));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                recordRejectedInputResult(key,
                                          target,
                                          rejectedInputExecuted,
                                          "mode=" + ctx.getOption(mode) + ", limit=" + ctx.getOption(limit));
            });
        }});

        return List.of(buildCommand(command, name + " -m normal -n 20 rejected"),
                       buildCommand(command, name + " -m parallel -n 20 control"));
    }

    public List<String> valueOptionWithoutValue() {
        String name = getMethodName();
        String key = getKey();

        putResult(new TestResult(key, TestStatus.FAILED, "Valid control input was not executed."));
        AtomicBoolean rejectedInputExecuted = new AtomicBoolean();
        command.addChildren(new Command(name) {{
            CommandOption<String, CommandContext> format = option(Options.string("format", 'F', "text"));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                recordRejectedInputResult(key, target, rejectedInputExecuted, "format=" + ctx.getOption(format));
            });
            execute(ctx -> recordRejectedInputResult(key, "rejected", rejectedInputExecuted, "without target"));
        }});

        return List.of(buildCommand(command, name + " --format"),
                       buildCommand(command, name + " --format json control"));
    }

    public List<String> optionAfterArgument() {
        String name = getMethodName();
        String key = getKey();

        putResult(new TestResult(key, TestStatus.FAILED, "Valid control input was not executed."));
        AtomicBoolean rejectedInputExecuted = new AtomicBoolean();
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                recordRejectedInputResult(key, target, rejectedInputExecuted, "force=" + ctx.getOption(force));
            });
        }});

        return List.of(buildCommand(command, name + " rejected -f"), buildCommand(command, name + " -f control"));
    }

    public List<String> countAndRepeatableOptions() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Integer, CommandContext> verbose = option(Options.count("verbose", 'v', 3));
            CommandOption<List<String>, CommandContext> tags = option(Options.string("tag", 't', "")
                                                                             .repeatable(3));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key, ctx.getOption(verbose) + ":" + ctx.getOption(tags) + ":" + target, "3:[a, b]:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -vv -t a --tag b -v alex"));
    }

    /**
     * An option whose value is a platform argument, read back after the option redirects.
     */
    public List<String> argumentOption() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Player, CommandContext> player = option(Options.of("player",
                                                                             'p',
                                                                             new PlayerArgument("player")));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(player)
                                                          .getName()
                                  + ":" + target,
                          "true:" + playerName + ":alex");
            });
        }});

        return List.of(buildCommand(command, name + " -f --player " + playerName + " alex"));
    }

    public List<String> optionsAnywhere() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            optionsAnywhere();
            CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent", 's'));
            CommandOption<Integer, CommandContext> days = option(Options.integer("days", 'd', 1));

            argument(new StringArgument("target", StringArgument.Type.WORD),
                     new StringArgument("reason", StringArgument.Type.WORD)).execute((target, reason, ctx) -> {
                         putResult(key,
                                   ctx.getOption(silent) + ":" + ctx.getOption(days) + ":" + target + ":" + reason,
                                   "true:3:alex:spam");
                     });
        }});

        return List.of(buildCommand(command, name + " -s alex -d 3 spam"));
    }

    public List<String> aliasAndExclusiveOptions() {
        String name = getMethodName();
        String key = getKey();

        putResult(new TestResult(key, TestStatus.FAILED, "Valid control input was not executed."));
        AtomicBoolean rejectedInputExecuted = new AtomicBoolean();
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> quiet = option(Options.flag("quiet", 'q')
                                                                         .aliases("silent"));
            CommandOption<Boolean, CommandContext> loud = option(Options.flag("loud", 'l'));
            mutuallyExclusiveOptions(quiet, loud);

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                if (!ctx.getOption(quiet)) {
                    putResult(new TestResult(key, TestStatus.FAILED, "--silent did not give --quiet."));
                    return;
                }
                recordRejectedInputResult(key, target, rejectedInputExecuted, "quiet and loud");
            });
        }});

        return List.of(buildCommand(command, name + " -q -l rejected"),
                       buildCommand(command, name + " --silent control"));
    }

    public List<String> optionPermission() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> admin = option(Options.flag("admin", 'a')
                                                                         .permission(DefaultPermission.OP));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key, ctx.getOption(admin) + ":" + target, "true:alex");
            });
        }});

        return List.of(buildCommand(command, name + " --admin alex"));
    }

    /**
     * A command hidden by its predicate rejects the input until the predicate holds and the trees are sent again,
     * which the bot has to decode.
     */
    public List<String> visibleWhenRefresh() {
        String name = getMethodName();
        String key = getKey();

        putResult(new TestResult(key, TestStatus.FAILED, "Valid control input was not executed."));
        AtomicBoolean rejectedInputExecuted = new AtomicBoolean();
        AtomicBoolean visible = new AtomicBoolean();
        command.addChildren(new Command(name) {{
            visibleWhen(ctx -> visible.get());
            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                recordRejectedInputResult(key, target, rejectedInputExecuted, "the predicate false");
            });
        }}, new Command(name + "Show") {{
            execute(ctx -> {
                visible.set(true);
                CommandLib.refreshAll();
            });
        }});

        return List.of(buildCommand(command, name + " rejected"),
                       buildCommand(command, name + "Show"),
                       buildCommand(command, name + " control"));
    }

    private void optionPermissionProbe() {
        command.addChildren(new Command(OPTION_PERMISSION_PROBE) {{
            permission(PermissionDefault.TRUE);
            CommandOption<Boolean, CommandContext> secret = option(Options.flag("secret")
                                                                          .permission(DefaultPermission.NONE));
            execute(ctx -> ctx.sendFailure(OPTION_PERMISSION_PROBE + " secret=" + ctx.getOption(secret)));
        }});
    }

    private void recordRejectedInputResult(String key,
                                           String target,
                                           AtomicBoolean rejectedInputExecuted,
                                           String description) {
        if (!"control".equals(target)) {
            rejectedInputExecuted.set(true);
            putResult(new TestResult(key, TestStatus.FAILED, "Rejected input was executed with " + description));
            return;
        }
        if (!rejectedInputExecuted.get()) {
            putResult(new TestResult(key, TestStatus.SUCCEEDED, "Rejected input was not executed."));
        }
    }
}
