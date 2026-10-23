package dev.marulabs.testplugin;

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.argument.EnumArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.integration.core.JUnitXmlReport;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A smoke suite for Folia. The Paper suite schedules with the Bukkit scheduler, which Folia refuses, and dispatches
 * from one main thread, which Folia does not have, so this suite checks what differs on Folia: registration, the
 * threads of an async command for a player and for the console, and suggestions sent to a player. The bot runs
 * {@code runTests}, so the player check runs on the player's region thread.
 */
public class TestMain {
    private static final String NAME = "commandlibtest";
    private static final String SUGGESTION_BUFFER = "/" + NAME + " suggestionCapture abc";
    private static final String RUNNING_ON_FOLIA = "FoliaSmokeTest.runningOnFolia";
    private static final String PLAYER_ASYNC_THREAD = "FoliaSmokeTest.playerAsyncThread";
    private static final String CONSOLE_ASYNC_THREAD = "FoliaSmokeTest.consoleAsyncThread";
    private static final String SUGGESTION = "FoliaSmokeTest.suggestion";
    private static final long CONSOLE_CHECK_TIMEOUT_SECONDS = 30L;

    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, TestResult> results = new ConcurrentHashMap<>();
    private final AtomicReference<String> capturedLatestInput = new AtomicReference<>();
    private final AtomicReference<List<String>> sentSuggestions = new AtomicReference<>();
    private final CompletableFuture<Void> consoleChecked = new CompletableFuture<>();

    public TestMain(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void register() {
        try {
            for (String key : List.of(PLAYER_ASYNC_THREAD, CONSOLE_ASYNC_THREAD)) {
                fail(key, "The async command did not complete.");
            }
            putRunningOnFolia();
            Bukkit.getPluginManager()
                  .registerEvents(new Listener() {
                      @EventHandler
                      public void onSendSuggestions(AsyncPlayerSendSuggestionsEvent e) {
                          if (e.getBuffer()
                               .equals(SUGGESTION_BUFFER)) {
                              sentSuggestions.set(e.getSuggestions()
                                                   .getList()
                                                   .stream()
                                                   .map(Suggestion::getText)
                                                   .collect(Collectors.toList()));
                          }
                      }
                  }, plugin);
            CommandLib.register(plugin, mainCommand());
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "COMMANDLIB_TEST_PLUGIN_ENABLE_FAILED", e);
            throw e;
        }
    }

    private Command mainCommand() {
        Command main = new Command(NAME) {{
            permission(PermissionDefault.TRUE);
        }};
        main.addChildren(new Command("runTests") {{
            permission(PermissionDefault.TRUE);
            executeAsync(ctx -> checkAsyncThread(ctx).thenRun(() -> Bukkit.getGlobalRegionScheduler()
                                                                          .execute(plugin,
                                                                                   TestMain.this::checkConsoleAndReport)));
        }});
        main.addChildren(new Command("asyncThread") {{
            permission(PermissionDefault.TRUE);
            executeAsync(TestMain.this::checkAsyncThread);
        }});
        main.addChildren(new Command("suggestionCapture") {{
            permission(PermissionDefault.TRUE);
            argument(new StringArgument("a").addSuggestionProvider(builder -> {
                capturedLatestInput.set(builder.getLatestInput());
                builder.suggest("result");
            })).execute((ctx, a) -> {
            });
        }});
        // The commands below are the ones IntegrationTest sends from the bot and checks the messages of.
        main.addChildren(new Command("helpMessageRoot") {{
            addChildren(new Command("helpMessageChild") {{
                permission(PermissionDefault.TRUE);
                description("A child command for help message prefix testing.");
                execute(ctx -> {
                });
            }});
        }});
        main.addChildren(new Command("enumArgument") {{
            permission(PermissionDefault.TRUE);
            argument(new EnumArgument<>("a", Material.class)).execute((ctx, a) -> {
            });
        }});
        main.addChildren(new Command("testConvertCommandSyntaxException") {{
            permission(PermissionDefault.TRUE);
            argument(new IntegerTooHighArgument("a")).execute((ctx, a) -> {
            });
        }});
        main.addChildren(new Command("deniedProbe") {{
            permission(PermissionDefault.FALSE);
            execute(ctx -> ctx.sendFailure("deniedProbe executed"));
        }});
        return main;
    }

    /**
     * Runs work through the async executor and checks it on the sync executor: the work must run off the thread that
     * owns the sender, and the continuation on it, the player's region thread or the global region thread.
     */
    private CompletableFuture<Void> checkAsyncThread(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        String key = sender instanceof Player ? PLAYER_ASYNC_THREAD : CONSOLE_ASYNC_THREAD;
        return CompletableFuture.supplyAsync(() -> describeThread(sender), ctx.asyncExecutor())
                                .thenAcceptAsync(work -> {
                                    String continuation = describeThread(sender);
                                    String message = "work " + work + ", continuation " + continuation;
                                    if (!ownsSender(sender) || work.startsWith("owns")) {
                                        fail(key, message);
                                    } else {
                                        results.put(key, new TestResult(key, TestStatus.SUCCEEDED, message));
                                    }
                                }, ctx.syncExecutor())
                                .whenComplete((ignored, e) -> {
                                    if (key.equals(CONSOLE_ASYNC_THREAD)) {
                                        consoleChecked.complete(null);
                                    }
                                });
    }

    // Runs on the global region thread, the only thread Folia runs console commands on. The report is written when
    // the console's check completes, or after a timeout with the check's failure.
    private void checkConsoleAndReport() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), NAME + " asyncThread");
        consoleChecked.completeOnTimeout(null, CONSOLE_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                      .thenRun(() -> Bukkit.getGlobalRegionScheduler()
                                           .execute(plugin, this::writeReport));
    }

    private static String describeThread(CommandSender sender) {
        return (ownsSender(sender) ? "owns " : "does not own ") + sender.getName() + " on " + Thread.currentThread()
                                                                                                    .getName();
    }

    private static boolean ownsSender(CommandSender sender) {
        return sender instanceof Entity entity ? Bukkit.isOwnedByCurrentRegion(entity) : Bukkit.isGlobalTickThread();
    }

    private void putRunningOnFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            results.put(RUNNING_ON_FOLIA, new TestResult(RUNNING_ON_FOLIA, TestStatus.SUCCEEDED, Bukkit.getName()));
        } catch (ClassNotFoundException e) {
            fail(RUNNING_ON_FOLIA, "The server is not Folia: " + Bukkit.getName());
        }
    }

    private void checkSuggestion() {
        String input = capturedLatestInput.get();
        List<String> sent = sentSuggestions.get();
        if ("abc".equals(input) && sent != null && sent.contains("result")) {
            results.put(SUGGESTION, new TestResult(SUGGESTION, TestStatus.SUCCEEDED, "Sent " + sent));
        } else {
            fail(SUGGESTION, "Provider input " + input + ", suggestions sent to the player " + sent);
        }
    }

    private void fail(String key, String message) {
        results.put(key, new TestResult(key, TestStatus.FAILED, message));
    }

    private void writeReport() {
        checkSuggestion();
        List<TestResult> list = new ArrayList<>(results.values());
        Path path = Paths.get(System.getProperty("commandlib.testReportDir", "test-results"))
                         .resolve(System.getProperty("commandlib.testReportName", "TEST-commandlib-folia.xml"));
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, JUnitXmlReport.build(list));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        list.stream()
            .filter(result -> result.status() == TestStatus.FAILED)
            .forEach(result -> logger.severe(result.key() + ": " + result.message()));
        list.stream()
            .filter(result -> result.status() == TestStatus.SUCCEEDED)
            .forEach(result -> logger.info(result.key() + ": " + result.message()));
        long failed = list.stream()
                          .filter(result -> result.status() == TestStatus.FAILED)
                          .count();
        logger.info(String.format("SUCCEEDED: %d  FAILED: %d", list.size() - failed, failed));
    }

    // Throws the exception of an integer above its maximum, so the bot receives the converted message.
    private static final class IntegerTooHighArgument extends Argument<Object, IntegerTooHighArgument> {
        IntegerTooHighArgument(String name) {
            super(name, BoolArgumentType.bool());
        }

        @Override
        public Object cast(Object parsedArgument) {
            return parsedArgument;
        }

        @Override
        protected Object parseImpl(CommandContext ctx) throws CommandSyntaxException {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh()
                                                            .create(1, 2);
        }
    }
}
