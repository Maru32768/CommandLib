package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.util.bukkit.BukkitUtil;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;

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
    /**
     * Command registered while the tests run, after the plugin was enabled, and dispatched after
     * {@code /minecraft:reload}. On Paper 1.20.6+ it has no COMMANDS lifecycle handler, so it is added again after the
     * reload has sent players their trees.
     */
    public static final String LATE_COMMAND = "commandliblate";

    private final Plugin plugin;
    private final String playerName;
    private final List<String> afterReloadCommands = new ArrayList<>();

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
        commands.addAll(executeStoreReadsResultAndFailure());
        commands.addAll(runtimeRegisteredCommandIsExecutableUntilUnregistered());
        commands.addAll(lateRegisteredCommandSurvivesReload());
        commands.addAll(unregisteringKeepsALabelTakenSince());
        commands.addAll(enableRegisteredCommandIsRemovedByUnregister());
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
     * {@code /execute store} stores the result a command sets, and 0 as the success of a command that fails.
     */
    public List<String> executeStoreReadsResultAndFailure() {
        String name = getMethodName();
        String key = getKey();
        String objective = "commandlibstore";

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name + "Result") {{
            execute(ctx -> {
                // Feedback goes through the server's source; a failure there would fail the command and store 0.
                ctx.sendSuccess("stored", true);
                ctx.setResult(42);
            });
        }}, new Command(name + "Failure") {{
            execute(ctx -> {
                throw new CommandPrerequisiteException("failure probe");
            });
        }}, new Command(name) {{
            execute(ctx -> {
                Objective scores = Bukkit.getScoreboardManager()
                                         .getMainScoreboard()
                                         .getObjective(objective);
                if (scores == null) {
                    putResult(key, "no objective", "an objective");
                    return;
                }
                String actual = scores.getScore("#result")
                                      .getScore()
                        + "," + scores.getScore("#success")
                                      .getScore()
                        + "," + scores.getScore("#vanilla")
                                      .getScore();
                // Spigot 1.20.3 stores 0 for every command, vanilla ones too; Paper has no 1.20.3.
                putResult(key, actual, "1.20.3".equals(BukkitUtil.getMinecraftVersion()) ? "0,0,0" : "42,0,7");
            });
        }});

        return List.of("scoreboard objectives add " + objective + " dummy",
                       "scoreboard players set #success " + objective + " 7",
                       "scoreboard players set #seven " + objective + " 7",
                       "execute store result score #vanilla " + objective + " run scoreboard players get #seven "
                               + objective,
                       "execute store result score #result " + objective + " run "
                               + buildCommand(command, name + "Result"),
                       "execute store success score #success " + objective + " run "
                               + buildCommand(command, name + "Failure"),
                       buildCommand(command, name));
    }

    /**
     * Registers, runs (also under its namespaced label) and unregisters a command in separate top-level dispatches.
     * Minecraft 1.20.3+ queues commands dispatched from inside another command until that command finishes, so the
     * steps cannot be nested.
     */
    public List<String> runtimeRegisteredCommandIsExecutableUntilUnregistered() {
        String name = getMethodName();
        String key = getKey();
        String commandName = "runtimeexec";
        String namespacedName = plugin.getName()
                                      .toLowerCase(Locale.ROOT)
                + ":" + commandName;
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
                if (executions.get() != 2) {
                    putResult(new TestResult(key,
                                             TestStatus.FAILED,
                                             "Registered command was not executed under both labels: executions="
                                                     + executions.get() + ": " + executedInputs));
                }
                CommandLib current = registration.getAndSet(null);
                if (current != null) {
                    current.unregister();
                }
            });
        }}, new Command(name) {{
            execute(ctx -> {
                if (executions.get() == 2) {
                    putResult(new TestResult(key,
                                             TestStatus.SUCCEEDED,
                                             "Runtime registration was executable until unregistered."));
                } else {
                    putResult(new TestResult(key,
                                             TestStatus.FAILED,
                                             "Expected exactly two executions before unregistering but was "
                                                     + executions.get() + ": " + executedInputs));
                }
            });
        }});

        return List.of(buildCommand(command, name + "Register"),
                       commandName,
                       namespacedName,
                       buildCommand(command, name + "Unregister"),
                       commandName,
                       namespacedName,
                       buildCommand(command, name));
    }

    /**
     * A second registration of the same label takes it over; unregistering the first must leave the second's command
     * under both labels.
     */
    public List<String> unregisteringKeepsALabelTakenSince() {
        String name = getMethodName();
        String key = getKey();
        String label = "commandlibowned";
        String namespacedLabel = namespacedLabel(label);
        List<String> executions = Collections.synchronizedList(new ArrayList<>());

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name + "Register") {{
            execute(ctx -> {
                CommandLib first = CommandLib.register(plugin, new Command(label) {{
                    permission(PermissionDefault.TRUE);
                    execute(inner -> executions.add("unregistered"));
                }});
                CommandLib.register(plugin, new Command(label) {{
                    permission(PermissionDefault.TRUE);
                    execute(inner -> executions.add("later"));
                }});
                first.unregister();
            });
        }}, new Command(name) {{
            // One execution per label.
            execute(ctx -> putResult(key, executions.toString(), "[later, later]"));
        }});

        return List.of(buildCommand(command, name + "Register"), label, namespacedLabel, buildCommand(command, name));
    }

    /**
     * A command registered while the plugin enables runs under both labels until it is unregistered, and stays
     * unknown under both after {@code /minecraft:reload}.
     */
    public List<String> enableRegisteredCommandIsRemovedByUnregister() {
        String name = getMethodName();
        String key = getKey();
        String label = "commandlibenabled";
        String namespacedLabel = namespacedLabel(label);
        AtomicInteger executions = new AtomicInteger();
        CommandLib registration = CommandLib.register(plugin, new Command(label) {{
            permission(PermissionDefault.TRUE);
            execute(inner -> executions.incrementAndGet());
        }});

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name + "Unregister") {{
            execute(ctx -> registration.unregister());
        }}, new Command(name) {{
            // One execution per label before unregistering, none after it or after the reload.
            execute(ctx -> putResult(key, String.valueOf(executions.get()), "2"));
        }});
        afterReloadCommands.addAll(List.of(label, namespacedLabel, buildCommand(command, name)));

        return List.of(label, namespacedLabel, buildCommand(command, name + "Unregister"), label, namespacedLabel);
    }

    @Override
    public List<String> afterReload() {
        return afterReloadCommands;
    }

    private String namespacedLabel(String label) {
        return plugin.getName()
                     .toLowerCase(Locale.ROOT)
                + ":" + label;
    }

    public List<String> lateRegisteredCommandSurvivesReload() {
        String name = getMethodName();
        String key = getKey();

        putResult(new TestResult(key, TestStatus.FAILED, LATE_COMMAND + " was not executed after /minecraft:reload."));
        command.addChildren(new Command(name) {{
            execute(ctx -> CommandLib.register(plugin, new Command(LATE_COMMAND) {{
                permission(PermissionDefault.TRUE);
                execute(inner -> putResult(new TestResult(key,
                                                          TestStatus.SUCCEEDED,
                                                          LATE_COMMAND + " survived /minecraft:reload.")));
            }}));
        }});

        return List.of(buildCommand(command, name));
    }

    private void deniedProbe() {
        command.addChildren(new Command(DENIED_PROBE) {{
            permission(PermissionDefault.FALSE);
            execute(ctx -> ctx.sendFailure(DENIED_PROBE + " executed"));
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
            execute(ctx -> putResult(new TestResult(key,
                                                    TestStatus.SUCCEEDED,
                                                    "Command survived /minecraft:reload.")));
        }});
    }
}
