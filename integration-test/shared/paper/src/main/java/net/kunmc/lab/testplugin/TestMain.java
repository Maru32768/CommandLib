package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.JUnitXmlReport;
import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandLib;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TestMain {
    private static final String TEST_PLAYER_NAME = "Maru32768";
    private static final long RELOAD_POLL_TICKS = 5L;
    private static final long RELOAD_TIMEOUT_TICKS = 20L * 120;
    private final Plugin plugin;
    private final Logger logger;
    private boolean errorOccurredOnRegister = false;


    public TestMain(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void register() {
        registerPluginDisableListener();
        register(results -> {
            outputResultsToJUnitXml(results);
            errorFailedResults(results);
            logTestResultCount(results);
        });
    }

    public void registerPluginDisableListener() {
        Bukkit.getPluginManager()
              .registerEvents(new Listener() {
                  @EventHandler
                  public void onPluginDisable(PluginDisableEvent e) {
                      if (e.getPlugin() == plugin && errorOccurredOnRegister) {
                          System.exit(1);
                      }
                  }
              }, plugin);
    }

    public void register(Consumer<List<TestResult>> consumer) {
        register(consumer, (commandLine, dispatchResult) -> {
        });
    }

    public void register(Consumer<List<TestResult>> consumer, CommandDispatchErrorHook dispatchErrorHook) {
        try {
            Command mainCommand = new MainCommand();
            mainCommand.permission(PermissionDefault.TRUE);
            AtomicBoolean running = new AtomicBoolean(false);
            CommandDispatchProbe dispatchProbe = new CommandDispatchProbe(plugin);

            ArgumentTest argumentTest = new ArgumentTest(mainCommand, TEST_PLAYER_NAME);
            OptionTest optionTest = new OptionTest(mainCommand);
            CommandSyntaxExceptionTest commandSyntaxExceptionTest = new CommandSyntaxExceptionTest(mainCommand);
            RuntimePermissionTest runtimePermissionTest = new RuntimePermissionTest(mainCommand, plugin);
            SuggestionTest suggestionTest = new SuggestionTest(mainCommand);
            ScenarioTest scenarioTest = new ScenarioTest(mainCommand, plugin, TEST_PLAYER_NAME);
            new HelpMessageTest(mainCommand); // registers helpMessageRoot for bot-side help message verification
            List<TestBase> tests = List.of(argumentTest,
                                           optionTest,
                                           commandSyntaxExceptionTest,
                                           runtimePermissionTest,
                                           suggestionTest,
                                           scenarioTest);
            List<String> commands = tests.stream()
                                         .flatMap(x -> x.build()
                                                        .stream())
                                         .collect(Collectors.toList());

            Runnable execute = () -> {
                logger.info("Received CommandLib test run request.");
                if (!running.compareAndSet(false, true)) {
                    logger.info("CommandLib test cases are already running.");
                    return;
                }

                Bukkit.getScheduler()
                      .runTaskLater(plugin, () -> {
                          boolean waitingForReload = false;
                          try {
                              Scoreboard scoreboard = Bukkit.getScoreboardManager()
                                                            .getMainScoreboard();
                              if (scoreboard.getTeam("test") == null) {
                                  scoreboard.registerNewTeam("test");
                              }

                              logger.info("Executing CommandLib test cases.");
                              for (String command : commands) {
                                  logger.info("Dispatching test command: " + command);
                                  CommandDispatchResult dispatchResult = dispatchProbe.dispatch(Bukkit.getConsoleSender(),
                                                                                                command);
                                  tests.forEach(test -> test.hookCommandDispatchError(command, dispatchResult));
                                  dispatchErrorHook.onCommandDispatchError(command, dispatchResult);
                              }

                              // /minecraft:reload rebuilds the command dispatcher. It runs last, and the probe waits
                              // for the reload to finish before the results are written.
                              logger.info("Reloading data packs to verify registered commands survive.");
                              ReloadWatcher reloadWatcher = new ReloadWatcher();
                              CommandDispatchResult reloadResult = dispatchProbe.dispatch(Bukkit.getConsoleSender(),
                                                                                          "minecraft:reload");
                              Runnable finish = () -> {
                                  try {
                                      consumer.accept(tests.stream()
                                                           .flatMap(x -> x.results()
                                                                          .stream())
                                                           .collect(Collectors.toList()));
                                      tests.forEach(TestBase::clearResults);
                                  } finally {
                                      running.set(false);
                                  }
                              };
                              if (!reloadResult.succeeded()) {
                                  scenarioTest.failReloadProbe("/minecraft:reload failed.\n" + reloadResult.describe(
                                          "minecraft:reload"));
                                  finish.run();
                                  return;
                              }
                              waitingForReload = true;
                              // The reload is applied asynchronously, so the probe waits until the reloaded resources
                              // are in place; probing earlier would hit the old dispatcher and pass without checking.
                              long[] waitedTicks = {0};
                              BukkitTask[] poll = new BukkitTask[1];
                              poll[0] = Bukkit.getScheduler()
                                              .runTaskTimer(plugin, () -> {
                                                  waitedTicks[0] += RELOAD_POLL_TICKS;
                                                  boolean reloaded = reloadWatcher.reloaded();
                                                  if (!reloaded && waitedTicks[0] < RELOAD_TIMEOUT_TICKS) {
                                                      return;
                                                  }
                                                  poll[0].cancel();
                                                  try {
                                                      if (reloaded) {
                                                          String probe = MainCommand.NAME + " " + ScenarioTest.RELOAD_PROBE;
                                                          CommandDispatchResult dispatchResult = dispatchProbe.dispatch(
                                                                  Bukkit.getConsoleSender(),
                                                                  probe);
                                                          tests.forEach(test -> test.hookCommandDispatchError(probe,
                                                                                                              dispatchResult));
                                                      } else {
                                                          scenarioTest.failReloadProbe(
                                                                  "/minecraft:reload did not finish within " + RELOAD_TIMEOUT_TICKS + " ticks.");
                                                      }
                                                  } finally {
                                                      finish.run();
                                                  }
                                              }, RELOAD_POLL_TICKS, RELOAD_POLL_TICKS);
                          } finally {
                              if (!waitingForReload) {
                                  running.set(false);
                              }
                          }
                      }, 1L);
            };

            mainCommand.addChildren(new Command("runTests") {{
                permission(PermissionDefault.TRUE);
                execute(ctx -> {
                    execute.run();
                });
            }});
            CommandLib.register(plugin, mainCommand);
        } catch (Throwable e) {
            errorOccurredOnRegister = true;
            logger.log(Level.SEVERE, "COMMANDLIB_TEST_PLUGIN_ENABLE_FAILED", e);
            throw e;
        }
    }

    public void outputResultsToJUnitXml(List<TestResult> results) {
        String reportName = System.getProperty("commandlib.testReportName",
                                               "TEST-commandlib-" + BukkitUtil.getMinecraftVersion() + ".xml");
        Path reportDir = Paths.get(System.getProperty("commandlib.testReportDir",
                                                      "../../../../shared/test-results"));
        Path path = reportDir.resolve(reportName);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, JUnitXmlReport.build(results));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void errorFailedResults(List<TestResult> results) {
        for (TestResult result : results) {
            if (result.status() == TestStatus.FAILED) {
                if (result.message()
                          .indexOf('\n') > -1) {
                    logger.log(Level.SEVERE,
                               result.key() + ": " + result.message()
                                                           .substring(0,
                                                                      result.message()
                                                                            .indexOf('\n')));
                } else {
                    logger.log(Level.SEVERE, result.key() + ": " + result.message());
                }
            }
        }
    }

    public void logTestResultCount(List<TestResult> results) {
        long succeededCount = results.stream()
                                     .filter(x -> x.status() == TestStatus.SUCCEEDED)
                                     .count();
        long failedCount = results.stream()
                                  .filter(x -> x.status() == TestStatus.FAILED)
                                  .count();
        logger.log(Level.INFO, String.format("SUCCEEDED: %d  FAILED: %d", succeededCount, failedCount));
    }
}
