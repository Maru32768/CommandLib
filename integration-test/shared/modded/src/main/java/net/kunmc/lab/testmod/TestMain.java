package net.kunmc.lab.testmod;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandLib;
import net.kunmc.lab.commandlib.DefaultPermission;
import net.kunmc.lab.integration.core.ExceptionUtil;
import net.kunmc.lab.integration.core.JUnitXmlReport;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class TestMain {
    static final String TEST_PLAYER_NAME = "Maru32768";
    static final String TEST_TEAM_NAME = "test";
    private static final Logger LOGGER = LogManager.getLogger(TestMain.class);
    private static final String REPORT_COMMAND = MainCommand.NAME + " reportTests";

    public void register() {
        try {
            Command mainCommand = new MainCommand();
            AtomicBoolean running = new AtomicBoolean(false);

            List<TestBase> tests = List.of(new ArgumentTest(mainCommand, TEST_PLAYER_NAME),
                                           new ActorTest(mainCommand),
                                           new SuggestionTest(mainCommand),
                                           new CommandSyntaxExceptionTest(mainCommand));
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
                    try {
                        reportTests(tests, dispatchErrors);
                    } finally {
                        dispatchErrors.clear();
                        running.set(false);
                    }
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

    // Runs the command through the same pipeline as a typed command.
    private static void perform(MinecraftServer server, CommandSourceStack source, String command) {
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
