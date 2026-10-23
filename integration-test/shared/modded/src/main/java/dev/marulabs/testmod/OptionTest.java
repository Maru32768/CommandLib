package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.CommandOption;
import dev.marulabs.commandlib.Options;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Option trees use redirects between option states; these run them on the server's dispatcher.
public final class OptionTest extends TestBase {
    private final AtomicBoolean visible = new AtomicBoolean();
    private final AtomicBoolean hiddenExecuted = new AtomicBoolean();

    public OptionTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return List.of(flagsAndValues(),
                       countAndRepeatableOptions(),
                       argumentOption(),
                       optionsAnywhere(),
                       visibleWhenRejected(),
                       visibleWhenShow());
    }

    private String flagsAndValues() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Boolean, CommandContext> verbose = option(Options.flag("verbose", 'v'));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((ctx, target) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(verbose) + ":" + ctx.getOption(limit) + ":"
                                  + target,
                          "true:true:20:alex");
            });
        }});

        return buildCommand(name + " -n 20 -vf alex");
    }

    private String countAndRepeatableOptions() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Integer, CommandContext> verbose = option(Options.count("verbose", 'v', 3));
            CommandOption<List<String>, CommandContext> tags = option(Options.string("tag", 't', "")
                                                                             .repeatable(3));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((ctx, target) -> {
                putResult(key, ctx.getOption(verbose) + ":" + ctx.getOption(tags) + ":" + target, "3:[a, b]:alex");
            });
        }});

        return buildCommand(name + " -vv -t a --tag b -v alex");
    }

    // The value is a platform argument, read back after the option redirects.
    private String argumentOption() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<ServerPlayer, CommandContext> player = option(Options.of("player",
                                                                                   'p',
                                                                                   new PlayerArgument("player")));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((ctx, target) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(player)
                                                          .getName()
                                                          .getString()
                                  + ":" + target,
                          "true:" + TestMain.TEST_PLAYER_NAME + ":alex");
            });
        }});

        return buildCommand(name + " -f --player " + TestMain.TEST_PLAYER_NAME + " alex");
    }

    private String optionsAnywhere() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            optionsAnywhere();
            CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent", 's'));
            CommandOption<Integer, CommandContext> days = option(Options.integer("days", 'd', 1));

            argument(new StringArgument("target", StringArgument.Type.WORD),
                     new StringArgument("reason", StringArgument.Type.WORD)).execute((ctx, target, reason) -> {
                         putResult(key,
                                   ctx.getOption(silent) + ":" + ctx.getOption(days) + ":" + target + ":" + reason,
                                   "true:3:alex:spam");
                     });
        }});

        return buildCommand(name + " -s alex -d 3 spam");
    }

    // A command hidden by its predicate rejects the input until the predicate holds; the trees are then sent again,
    // which the bot has to decode.
    private String visibleWhenRejected() {
        String key = getKey("visibleWhenRefresh");

        putResult(new TestResult(key, TestStatus.FAILED, "Valid control input was not executed."));
        command.addChildren(new Command("visibleWhenRefresh") {{
            visibleWhen(ctx -> visible.get());
            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((ctx, target) -> {
                if (!"control".equals(target)) {
                    hiddenExecuted.set(true);
                    putResult(new TestResult(key, TestStatus.FAILED, "Hidden command was executed."));
                } else if (!hiddenExecuted.get()) {
                    putResult(new TestResult(key, TestStatus.SUCCEEDED, "Hidden command was not executed."));
                }
            });
        }});

        return buildCommand("visibleWhenRefresh rejected");
    }

    private String visibleWhenShow() {
        command.addChildren(new Command("visibleWhenRefreshShow") {{
            execute(ctx -> {
                visible.set(true);
                CommandLib.refreshAll();
                // From 1.20.3 a command performed during another is parsed now and run after it, so the control
                // input is performed after the change rather than queued with the other test commands.
                TestMain.perform(ctx.getSender()
                                    .getServer(),
                                 ctx.getSender(),
                                 buildCommand("visibleWhenRefresh control"));
            });
        }});

        return buildCommand("visibleWhenRefreshShow");
    }
}
