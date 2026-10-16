package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.integration.core.ExceptionUtil;
import dev.marulabs.integration.core.JUnitXmlReport;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class TestMain {
    static final String TEST_PLAYER_NAME = "Maru32768";
    static final String TEST_TEAM_NAME = "test";
    static final String TEST_OBJECTIVE_NAME = "test";
    private static final Logger LOGGER = LogManager.getLogger(TestMain.class);
    private static final String REPORT_COMMAND = MainCommand.NAME + " reportTests";
    private static final long REPORT_DELAY_SECONDS = 2;
    private static final long RELOAD_POLL_MILLIS = 250;
    private static final long RELOAD_TIMEOUT_SECONDS = 120;

    public void register() {
        try {
            Command mainCommand = new MainCommand();
            AtomicBoolean running = new AtomicBoolean(false);

            ReloadTest reloadTest = new ReloadTest(mainCommand);
            List<TestBase> tests = List.of(reloadTest,
                                           new ArgumentTest(mainCommand, TEST_PLAYER_NAME),
                                           new ActorTest(mainCommand),
                                           new SuggestionTest(mainCommand),
                                           new CommandSyntaxExceptionTest(mainCommand),
                                           new OptionTest(mainCommand),
                                           new RuntimeFeatureTest(mainCommand),
                                           new PermissionTest(mainCommand));
            new HelpMessageTest(mainCommand);
            List<String> commands = tests.stream()
                                         .flatMap(x -> x.build()
                                                        .stream())
                                         .collect(Collectors.toList());

            List<TestResult> dispatchErrors = new ArrayList<>();

            mainCommand.addChildren(new Command("runTests") {{
                permission(DefaultPermission.ALL);
                execute(ctx -> {
                    LOGGER.info("Received CommandLib test run request.");
                    if (!running.compareAndSet(false, true)) {
                        LOGGER.info("CommandLib test cases are already running.");
                        return;
                    }
                    try {
                        MinecraftServer server = ctx.getSender()
                                                    .getServer();
                        runTests(server, commands, dispatchErrors);
                        // On 1.20.3+ commands performed while another command runs are queued until it finishes.
                        // The report command is queued after the test commands, so it collects the results once
                        // they have all run. Older versions perform each command immediately.
                        perform(server, server.createCommandSourceStack(), REPORT_COMMAND);
                    } catch (Throwable e) {
                        running.set(false);
                        throw e;
                    }
                });
            }});
            mainCommand.addChildren(new Command("reportTests") {{
                permission(DefaultPermission.ALL);
                execute(ctx -> {
                    if (!running.get()) {
                        LOGGER.info("No CommandLib test run to report.");
                        return;
                    }
                    // Asynchronous commands complete on a later tick, so the report waits for them.
                    MinecraftServer server = ctx.getSender()
                                                .getServer();
                    Runnable report = () -> {
                        try {
                            reportTests(tests, dispatchErrors);
                        } finally {
                            dispatchErrors.clear();
                            running.set(false);
                        }
                    };
                    CompletableFuture.delayedExecutor(REPORT_DELAY_SECONDS, TimeUnit.SECONDS, server)
                                     .execute(() -> reloadThenReport(server, reloadTest, report));
                });
            }});

            CommandLib.register(TestMod.MOD_ID, mainCommand);
        } catch (Throwable e) {
            LOGGER.error("COMMANDLIB_TEST_PLUGIN_ENABLE_FAILED", e);
            throw e;
        }
    }

    private static void runTests(MinecraftServer server, List<String> commands, List<TestResult> dispatchErrors) {
        Scoreboard scoreboard = server.getScoreboard();
        if (scoreboard.getPlayerTeam(TEST_TEAM_NAME) == null) {
            scoreboard.addPlayerTeam(TEST_TEAM_NAME);
        }
        if (scoreboard.getObjective(TEST_OBJECTIVE_NAME) == null) {
            // Component.nullToEmpty is client-only on 1.16.5, which the dedicated server leaves out.
            //? if >=1.19 {
            Component displayName = Component.literal(TEST_OBJECTIVE_NAME);
            //?} else
            /*Component displayName = new net.minecraft.network.chat.TextComponent(TEST_OBJECTIVE_NAME);*/
            //? if >=1.20.3 {
            /*scoreboard.addObjective(TEST_OBJECTIVE_NAME,
                                    ObjectiveCriteria.DUMMY,
                                    displayName,
                                    ObjectiveCriteria.RenderType.INTEGER,
                                    false,
                                    null);
            *///?} else {
            scoreboard.addObjective(TEST_OBJECTIVE_NAME,
                                    ObjectiveCriteria.DUMMY,
                                    displayName,
                                    ObjectiveCriteria.RenderType.INTEGER);
            //?}
        }

        LOGGER.info("Executing CommandLib test cases.");
        CommandSourceStack console = server.createCommandSourceStack();
        for (String command : commands) {
            LOGGER.info("Dispatching test command: " + command);
            try {
                perform(server, console, command);
            } catch (Throwable e) {
                dispatchErrors.add(new TestResult("Dispatch." + command,
                                                  TestStatus.FAILED,
                                                  ExceptionUtil.stackTraceToString(e)));
            }
        }
    }

    // /reload builds a new command dispatcher. It runs last, and the probe waits until the server has swapped in the
    // reloaded resources; probing earlier would reach the old dispatcher and pass without checking.
    private static void reloadThenReport(MinecraftServer server, ReloadTest reloadTest, Runnable report) {
        try {
            LOGGER.info("Reloading data packs to verify registered commands survive.");
            Commands before = server.getCommands();
            perform(server, server.createCommandSourceStack(), "reload");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(RELOAD_TIMEOUT_SECONDS);
            probeAfterReload(server, before, deadline, reloadTest, report);
        } catch (Throwable e) {
            reloadTest.fail("/reload failed: " + ExceptionUtil.stackTraceToString(e));
            report.run();
        }
    }

    private static void probeAfterReload(MinecraftServer server,
                                         Commands before,
                                         long deadline,
                                         ReloadTest reloadTest,
                                         Runnable report) {
        try {
            if (server.getCommands() == before) {
                if (System.nanoTime() < deadline) {
                    CompletableFuture.delayedExecutor(RELOAD_POLL_MILLIS, TimeUnit.MILLISECONDS, server)
                                     .execute(() -> probeAfterReload(server, before, deadline, reloadTest, report));
                    return;
                }
                reloadTest.fail("/reload did not finish within " + RELOAD_TIMEOUT_SECONDS + " seconds.");
            } else {
                perform(server, server.createCommandSourceStack(), reloadTest.probeCommand());
            }
        } catch (Throwable e) {
            reloadTest.fail(ExceptionUtil.stackTraceToString(e));
        }
        report.run();
    }

    // Runs the command through the same pipeline as a typed command.
    static void perform(MinecraftServer server, CommandSourceStack source, String command) {
        //? if >=1.19 {
        server.getCommands()
              .performPrefixedCommand(source, command);
        //?} else
        /*server.getCommands().performCommand(source, command);*/
    }

    private static void reportTests(List<TestBase> tests, List<TestResult> dispatchErrors) {
        List<TestResult> results = tests.stream()
                                        .flatMap(x -> x.results()
                                                       .stream())
                                        .collect(Collectors.toCollection(ArrayList::new));
        results.addAll(dispatchErrors);
        tests.forEach(TestBase::clearResults);

        writeReport(results);
        results.stream()
               .filter(x -> x.status() == TestStatus.FAILED)
               .forEach(x -> LOGGER.error(x.key() + ": " + x.message()));
        long succeeded = results.stream()
                                .filter(x -> x.status() == TestStatus.SUCCEEDED)
                                .count();
        LOGGER.info(String.format("SUCCEEDED: %d  FAILED: %d", succeeded, results.size() - succeeded));
    }

    private static void writeReport(List<TestResult> results) {
        String reportName = System.getProperty("commandlib.testReportName", "TEST-commandlib-forge.xml");
        Path reportDir = Paths.get(System.getProperty("commandlib.testReportDir", "test-results"));
        Path path = reportDir.resolve(reportName);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, JUnitXmlReport.build(results));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
