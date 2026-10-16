package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;

import java.util.List;

/**
 * Checks that the commands survive /reload, which builds a new command dispatcher. {@link TestMain} performs the
 * probe after the reload has replaced the dispatcher, so it reaches the new one.
 */
public final class ReloadTest extends TestBase {
    private static final String PROBE = "reloadProbe";

    public ReloadTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        fail("Command was not executed after /reload.");
        command.addChildren(new Command(PROBE) {{
            execute(ctx -> putResult(new TestResult(getKey(PROBE), TestStatus.SUCCEEDED, "Command survived /reload.")));
        }});
        return List.of();
    }

    String probeCommand() {
        return buildCommand(PROBE);
    }

    void fail(String message) {
        putResult(new TestResult(getKey(PROBE), TestStatus.FAILED, message));
    }
}
