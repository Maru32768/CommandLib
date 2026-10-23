package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.Captions;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.CommandLibDiagnostics;
import dev.marulabs.integration.core.ExceptionUtil;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Asynchronous execution and middlewares on the server's scheduler, the diagnostics, and captions.
 */
public final class RuntimeFeatureTest extends TestBase {
    /**
     * Sub command that only the console may run. The bot checks that it gets the English text of the caption, which
     * is looked up for the player's locale.
     */
    public static final String CAPTION_PROBE = "captionProbe";
    public static final String CAPTION_PROBE_TEXT = "captionProbe English text";

    public RuntimeFeatureTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.addAll(executeAsyncAndMiddleware());
        commands.addAll(diagnostics());
        captionProbe();

        return commands;
    }

    /**
     * The work runs off the server thread, and the stages chained on it, the executor and the middleware run on it.
     * The result is written after the reload, so the work has finished by then.
     */
    public List<String> executeAsyncAndMiddleware() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        command.addChildren(new Command(name) {{
            addMiddleware((ctx, next) -> {
                events.add("before:" + Bukkit.isPrimaryThread());
                return next.proceed()
                           .whenComplete((ignored, e) -> {
                               events.add("after:" + Bukkit.isPrimaryThread() + ":" + (e == null));
                               putResult(key,
                                         String.join(",", events),
                                         "before:true,execute:true,work:false,then:true,after:true:true");
                           });
            });
            executeAsync(ctx -> {
                events.add("execute:" + Bukkit.isPrimaryThread());
                return ctx.supplyAsync(() -> {
                    events.add("work:" + Bukkit.isPrimaryThread());
                    return null;
                })
                          .thenAccept(ignored -> events.add("then:" + Bukkit.isPrimaryThread()));
            });
        }});

        return List.of(buildCommand(command, name));
    }

    /**
     * The report names the test commands. The bot runs the diagnostics command and checks the lines it receives.
     */
    public List<String> diagnostics() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            execute(ctx -> {
                try {
                    CommandLibDiagnostics diagnostics = CommandLib.diagnostics();
                    String report = diagnostics.report();
                    List<String> problems = new ArrayList<>();
                    if (!report.contains(MainCommand.NAME)) {
                        problems.add("the report does not name /" + MainCommand.NAME);
                    }
                    if (report.contains("NMS problem")) {
                        problems.add("the report names an NMS problem");
                    }
                    putResult(problems.isEmpty() ? new TestResult(key, TestStatus.SUCCEEDED, "Diagnostics work.")
                            : new TestResult(key, TestStatus.FAILED, problems + "\n" + report));
                } catch (Throwable e) {
                    putResult(new TestResult(key, TestStatus.FAILED, ExceptionUtil.stackTraceToString(e)));
                }
            });
        }});

        return List.of(buildCommand(command, name));
    }

    private void captionProbe() {
        command.addChildren(new Command(CAPTION_PROBE) {{
            permission(PermissionDefault.TRUE);
            captions(Captions.create()
                             .set(Caption.REQUIRE_CONSOLE, "captionProbe root text")
                             .set(Locale.ENGLISH, Caption.REQUIRE_CONSOLE, CAPTION_PROBE_TEXT));
            addPrerequisite(Command.console());
            execute(ctx -> ctx.sendFailure(CAPTION_PROBE + " executed"));
        }});
    }
}
