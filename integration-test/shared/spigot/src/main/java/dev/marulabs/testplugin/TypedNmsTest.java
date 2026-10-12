package dev.marulabs.testplugin;

import dev.marulabs.integration.core.ExceptionUtil;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;

import java.util.List;

/**
 * Checks that every class of the typed NMS modules for this server version links. A wrapper that no test reaches would
 * otherwise only fail when a command first uses it.
 */
public class TypedNmsTest extends TestBase {
    public TypedNmsTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return everyTypedNmsClassLinks();
    }

    public List<String> everyTypedNmsClassLinks() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                try {
                    putResult(key, String.valueOf(NMSClassRegistry.unavailableClasses()), "[]");
                } catch (RuntimeException e) {
                    putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
                }
            });
        }});

        return List.of(buildCommand(command, name));
    }
}
