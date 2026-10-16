package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.util.CurrentServer;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Asynchronous execution and middlewares on the server thread, and the diagnostics. The report is written a while
// after the commands ran, so the asynchronous work has finished by then.
public final class RuntimeFeatureTest extends TestBase {
    public RuntimeFeatureTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return List.of(executeAsyncAndMiddleware(), diagnostics());
    }

    private String executeAsyncAndMiddleware() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        command.addChildren(new Command(name) {{
            addMiddleware((ctx, next) -> {
                events.add("before:" + onServerThread());
                return next.proceed()
                           .whenComplete((ignored, e) -> {
                               events.add("after:" + onServerThread() + ":" + (e == null));
                               putResult(key,
                                         String.join(",", events),
                                         "before:true,execute:true,work:false,then:true,after:true:true");
                           });
            });
            executeAsync(ctx -> {
                events.add("execute:" + onServerThread());
                return ctx.supplyAsync(() -> {
                    events.add("work:" + onServerThread());
                    return null;
                })
                          .thenAccept(ignored -> events.add("then:" + onServerThread()));
            });
        }});

        return buildCommand(name);
    }

    private String diagnostics() {
        String name = getMethodName();
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                String report = CommandLib.diagnostics()
                                          .report();
                putResult(report.contains(MainCommand.NAME)
                        ? new TestResult(key, TestStatus.SUCCEEDED, "Diagnostics work.")
                        : new TestResult(key,
                                         TestStatus.FAILED,
                                         "The report does not name the command:\n" + report));
            });
        }});

        return buildCommand(name);
    }

    private static boolean onServerThread() {
        return CurrentServer.get()
                            .isSameThread();
    }
}
