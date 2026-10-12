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
import java.util.Collections;
import java.util.List;

/**
 * Checks that a data pack function can run a CommandLib command after /minecraft:reload, which compiles functions
 * against fresh vanilla commands. The data pack is written into the world while the plugin enables; the reload
 * discovers and enables it.
 */
public final class DataPackFunctionTest extends TestBase {
    public static final String FUNCTION = "commandlibtest:probe";
    private static final String PROBE = "functionProbe";

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

        return Collections.emptyList();
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
