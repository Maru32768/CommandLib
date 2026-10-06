package net.kunmc.lab.testplugin;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandLib;
import net.kunmc.lab.commandlib.argument.StringArgument;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Command flows that depend on the real server command system rather than on argument parsing.
 */
public final class ScenarioTest extends TestBase {
    /**
     * Sub command that ops cannot use. The bot checks that it is rejected, since the console has every permission.
     */
    public static final String DENIED_PROBE = "deniedProbe";
    /**
     * Sub command dispatched after {@code /minecraft:reload} to check that the command tree survives the reload.
     */
    public static final String RELOAD_PROBE = "reloadProbe";

    private final Plugin plugin;
    private final String playerName;

    public ScenarioTest(Command command, Plugin plugin, String playerName) {
        super(command);
        this.plugin = plugin;
        this.playerName = playerName;
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.addAll(executeRunAsPlayer());
        commands.addAll(executeRunWithArgument());
        commands.addAll(runtimeRegisteredCommandIsExecutableUntilUnregistered());
        deniedProbe();
        reloadProbe();

        return commands;
    }

    public List<String> executeRunAsPlayer() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            // "execute as" changes the executing entity, but the Bukkit sender stays the console that started it.
            execute(ctx -> putResult(key,
                                     ctx.getSender()
                                        .getName(),
                                     Bukkit.getConsoleSender()
                                           .getName()));
        }});

        return List.of("execute as " + playerName + " run " + buildCommand(command, name));
    }

    public List<String> executeRunWithArgument() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new StringArgument("value", StringArgument.Type.WORD)).execute((value, ctx) -> {
                putResult(key,
                          ctx.getSender()
                             .getName() + ":" + value,
                          Bukkit.getConsoleSender()
                                .getName() + ":hello");
            });
        }});

        return List.of("execute as " + playerName + " run " + buildCommand(command, name + " hello"));
    }

    /**
     * Registers, runs and unregisters a command in separate top-level dispatches. Minecraft 1.20.3+ queues commands
     * dispatched from inside another command until that command finishes, so the steps cannot be nested.
     */
    public List<String> runtimeRegisteredCommandIsExecutableUntilUnregistered() {
        String name = getMethodName();
        String key = getKey();
        String commandName = "runtimeexec";
        String namespacedName = plugin.getName()
                                      .toLowerCase(Locale.ROOT) + ":" + commandName;
        AtomicInteger executions = new AtomicInteger();
        List<String> executedInputs = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<CommandLib> registration = new AtomicReference<>();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name + "Register") {{
            execute(ctx -> registration.set(CommandLib.register(plugin, new Command(commandName) {{
                permission(PermissionDefault.TRUE);
                execute(inner -> {
                    executions.incrementAndGet();
                    executedInputs.add(inner.getHandle()
                                            .getInput());
                });
            }})));
        }}, new Command(name + "Unregister") {{
            execute(ctx -> {
                if (executions.get() != 1) {
                    putResult(new TestResult(key,
                                             TestStatus.FAILED,
                                             "Registered command was not executed: executions=" + executions.get()));
                }
                CommandLib current = registration.getAndSet(null);
                if (current != null) {
                    current.unregister();
                }
            });
        }}, new Command(name) {{
            execute(ctx -> {
                if (executions.get() == 1) {
                    putResult(new TestResult(key,
                                             TestStatus.SUCCEEDED,
                                             "Runtime registration was executable until unregistered."));
                } else {
                    putResult(new TestResult(key,
                                             TestStatus.FAILED,
                                             "Expected exactly one execution before unregistering but was " + executions.get() + ": " + executedInputs));
                }
            });
        }});

        return List.of(buildCommand(command, name + "Register"),
                       commandName,
                       buildCommand(command, name + "Unregister"),
                       commandName,
                       namespacedName,
                       buildCommand(command, name));
    }

    private void deniedProbe() {
        String key = getClass().getSimpleName() + "." + DENIED_PROBE;

        command.addChildren(new Command(DENIED_PROBE) {{
            permission(PermissionDefault.FALSE);
            execute(ctx -> {
                putResult(new TestResult(key, TestStatus.FAILED, "Command without permission was executed."));
                ctx.sendFailure(DENIED_PROBE + " executed");
            });
        }});
    }

    /**
     * Records why the reload probe could not be dispatched, such as a failed or unfinished reload.
     */
    public void failReloadProbe(String message) {
        putResult(new TestResult(reloadProbeKey(), TestStatus.FAILED, message));
    }

    private String reloadProbeKey() {
        return getClass().getSimpleName() + "." + RELOAD_PROBE;
    }

    private void reloadProbe() {
        String key = reloadProbeKey();

        putResult(new TestResult(key, TestStatus.FAILED, "Command was not executed after /minecraft:reload."));
        command.addChildren(new Command(RELOAD_PROBE) {{
            execute(ctx -> putResult(new TestResult(key, TestStatus.SUCCEEDED, "Command survived /minecraft:reload.")));
        }});
    }
}
