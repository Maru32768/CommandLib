package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * A sub command that no player may use, not even an operator. The console runs it here, which shows that it is
 * registered, so the integration runner's check that the bot is refused it cannot pass on an unknown sub command.
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
        command.addChildren(new Command(DENIED_PROBE) {{
            permission(DefaultPermission.NONE);
            execute(ctx -> {
                if (ctx.getSender()
                       .getEntity() instanceof ServerPlayer) {
                    putResult(new TestResult(key, TestStatus.FAILED, "A player executed " + DENIED_PROBE + "."));
                    ctx.sendFailure(DENIED_PROBE + " executed");
                    return;
                }
                putResult(new TestResult(key, TestStatus.SUCCEEDED, "The console may run " + DENIED_PROBE + "."));
            });
        }});
        return List.of(buildCommand(DENIED_PROBE));
    }
}
