package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;

import java.util.List;

/**
 * Checks that the typed NMS classes bundled for this server version are the ones NMSClassRegistry selects. The registry
 * falls back to the reflection implementations when a typed class does not link or does not match the plugin's
 * mappings, so the other tests pass either way; this test fails when that fallback hides a broken typed class.
 */
public class TypedNmsTest extends TestBase {
    public TypedNmsTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return typedNmsClassesAreSelected();
    }

    public List<String> typedNmsClassesAreSelected() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                try {
                    // Only wrappers initialized so far are registered, and no other test reads the source's
                    // location, which initializes the position and rotation wrappers.
                    ctx.getLocation();
                    putResult(key, String.valueOf(NMSClassRegistry.typedFallbacks()), "[]");
                } catch (RuntimeException e) {
                    putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
                }
            });
        }});

        return List.of(buildCommand(command, name));
    }
}
