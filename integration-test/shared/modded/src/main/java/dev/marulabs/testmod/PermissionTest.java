package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A sub command that no player may use, not even an operator. The console runs it here, which shows that it is
 * registered, so the integration runner's check that the bot is refused it cannot pass on an unknown sub command.
 * A command block and a non-player entity that {@code /execute as} runs as are refused it as well.
 */
public final class PermissionTest extends TestBase {
    private static final String DENIED_PROBE = "deniedProbe";

    public PermissionTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        String key = getKey(DENIED_PROBE);
        putCommandNotExecutedResult(key);
        // A failure stays: the console's later run must not replace it.
        AtomicBoolean refusedElsewhere = new AtomicBoolean(true);
        command.addChildren(new Command(DENIED_PROBE) {{
            permission(DefaultPermission.NONE);
            execute(ctx -> {
                if (!ctx.getActor()
                        .isConsole()) {
                    putResult(new TestResult(key,
                                             TestStatus.FAILED,
                                             "A " + ctx.getActor()
                                                       .getType() + " executed " + DENIED_PROBE + "."));
                    ctx.sendFailure(DENIED_PROBE + " executed");
                    refusedElsewhere.set(false);
                    return;
                }
                if (refusedElsewhere.get()) {
                    putResult(new TestResult(key, TestStatus.SUCCEEDED, "The console may run " + DENIED_PROBE + "."));
                }
            });
        }});
        return List.of(buildCommand(DENIED_PROBE),
                       "summon minecraft:armor_stand ~ ~ ~ {Tags:[\"commandlibprobe\"],NoGravity:1b}",
                       "execute as @e[type=minecraft:armor_stand,tag=commandlibprobe,limit=1] run "
                               + buildCommand(DENIED_PROBE),
                       ActorTest.commandBlock(buildCommand(DENIED_PROBE), 5));
    }
}
