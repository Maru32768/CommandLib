package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks that a data pack function can run a CommandLib command after /minecraft:reload, which compiles functions
 * against fresh vanilla commands. The data pack is written into the world while the plugin enables; the reload
 * discovers and enables it. It also has a function with an unknown subcommand, {@value #BROKEN_FUNCTION}, whose error
 * CommandLib's own compilation must still log, which the integration test checks in the server log. A function tag,
 * {@value #TAG}, and {@code #minecraft:load} list functions that use a command as well: the reload runs the load
 * function once, and the tag is run after the reload.
 */
public final class DataPackFunctionTest extends TestBase {
    public static final String FUNCTION = "commandlibtest:probe";
    public static final String BROKEN_FUNCTION = "commandlibtest:broken";
    public static final String TAG = "#commandlibtest:probes";
    private static final String PROBE = "functionProbe";
    private static final String TAG_PROBE = "functionTagProbe";
    private static final String LOAD_PROBE = "functionLoadProbe";
    private static final String LOAD_COUNT = "functionLoadCount";
    private static final String LOAD_RESET = "functionLoadReset";
    private final AtomicInteger loadRuns = new AtomicInteger();
    private final AtomicReference<String> loadActor = new AtomicReference<>("none");

    public DataPackFunctionTest(Command command) {
        super(command);
        writeDataPack();
    }

    @Override
    public List<String> build() {
        String key = getClass().getSimpleName() + "." + PROBE;

        putResult(new TestResult(key, TestStatus.FAILED, "Function " + FUNCTION + " did not run the command."));
        command.addChildren(new Command(PROBE) {{
            permission(PermissionDefault.TRUE);
            execute(ctx -> putResult(new TestResult(key, TestStatus.SUCCEEDED, "Function ran the command.")));
        }});
        functionTagProbe();
        functionLoadProbe();

        // A server that starts with the data pack, such as a Mohist image made from a bootstrap run, has run
        // #minecraft:load at startup, so only the runs after the test commands count.
        return List.of(command.name() + " " + LOAD_RESET);
    }

    @Override
    public List<String> afterReload() {
        return List.of("function " + TAG, command.name() + " " + LOAD_COUNT);
    }

    @Override
    public boolean readyAfterReload() {
        return loadRuns.get() > 0;
    }

    private void functionTagProbe() {
        String key = getClass().getSimpleName() + "." + TAG_PROBE;

        putResult(new TestResult(key, TestStatus.FAILED, "Function tag " + TAG + " did not run the command."));
        command.addChildren(new Command(TAG_PROBE) {{
            permission(PermissionDefault.TRUE);
            execute(ctx -> putResult(new TestResult(key,
                                                    TestStatus.SUCCEEDED,
                                                    "Function tag ran the command as " + ctx.getActor()
                                                                                            .getType()
                                                            + ".")));
        }});
    }

    /**
     * The reload runs #minecraft:load once on the tick after it, which has to run the command: CommandLib compiles the
     * functions again with its commands before the server runs them in that tick.
     */
    private void functionLoadProbe() {
        String key = getClass().getSimpleName() + "." + LOAD_COUNT;

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(LOAD_PROBE) {{
            permission(PermissionDefault.TRUE);
            execute(ctx -> {
                loadRuns.incrementAndGet();
                loadActor.set(String.valueOf(ctx.getActor()
                                                .getType()));
            });
        }}, new Command(LOAD_RESET) {{
            execute(ctx -> loadRuns.set(0));
        }}, new Command(LOAD_COUNT) {{
            execute(ctx -> {
                if (loadRuns.get() == 1) {
                    putResult(new TestResult(key,
                                             TestStatus.SUCCEEDED,
                                             "#minecraft:load ran the command once as " + loadActor.get() + "."));
                } else {
                    putResult(new TestResult(key,
                                             TestStatus.FAILED,
                                             "Expected #minecraft:load to run the command once but it ran "
                                                     + loadRuns.get() + " times."));
                }
            });
        }});
    }

    private void writeDataPack() {
        Path pack = Bukkit.getWorlds()
                          .get(0)
                          .getWorldFolder()
                          .toPath()
                          .resolve("datapacks")
                          .resolve("commandlibtest");
        String function = command.name() + " " + PROBE + "\n";
        try {
            // pack_format 6 is 1.16.5's; later versions load the pack anyway and read supported_formats.
            write(pack.resolve("pack.mcmeta"),
                  "{\"pack\":{\"pack_format\":6,\"supported_formats\":{\"min_inclusive\":6,\"max_inclusive\":99},"
                          + "\"description\":\"CommandLib integration test\"}}");
            // The directory is "function" from 1.21 and "functions" before.
            write(pack.resolve("data/commandlibtest/functions/probe.mcfunction"), function);
            write(pack.resolve("data/commandlibtest/function/probe.mcfunction"), function);
            String broken = command.name() + " noSuchSubcommand\n";
            write(pack.resolve("data/commandlibtest/functions/broken.mcfunction"), broken);
            write(pack.resolve("data/commandlibtest/function/broken.mcfunction"), broken);
            writeFunction(pack, "tagged", command.name() + " " + TAG_PROBE + "\n");
            writeFunction(pack, "load", command.name() + " " + LOAD_PROBE + "\n");
            writeFunction(pack, ScenarioTest.DENIED_STORE_FUNCTION, ScenarioTest.DENIED_STORE_FUNCTION_BODY);
            writeTag(pack, "commandlibtest", "probes", "commandlibtest:tagged");
            writeTag(pack, "minecraft", "load", "commandlibtest:load");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeFunction(Path pack, String name, String content) throws IOException {
        write(pack.resolve("data/commandlibtest/functions/" + name + ".mcfunction"), content);
        write(pack.resolve("data/commandlibtest/function/" + name + ".mcfunction"), content);
    }

    // The tag directory is "function" from 1.21 and "functions" before, like the functions.
    private static void writeTag(Path pack, String namespace, String name, String function) throws IOException {
        String tag = "{\"values\":[\"" + function + "\"]}";
        write(pack.resolve("data/" + namespace + "/tags/functions/" + name + ".json"), tag);
        write(pack.resolve("data/" + namespace + "/tags/function/" + name + ".json"), tag);
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
