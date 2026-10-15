package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Confirm;
import dev.marulabs.commandlib.annotation.Default;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Flag;
import dev.marulabs.commandlib.annotation.Range;
import dev.marulabs.commandlib.annotation.Subcommand;
import dev.marulabs.commandlib.annotation.Suggest;

import java.util.Map;
import java.util.TreeMap;

/**
 * The annotation style, registered with {@code AnnotatedCommands.parse(new ArenaCommand())}.
 *
 * <pre>
 * /arena create [-f|--force] &lt;name&gt; [size]
 * /arena delete &lt;name&gt;
 * /arena list
 * </pre>
 */
@Command("arena")
@Description("Manage arenas")
public final class ArenaCommand {
    private final Map<String, Integer> arenas = new TreeMap<>();

    // Argument names come from the parameter names, so build.gradle.kts compiles with -parameters.
    @Subcommand("create")
    @Description("Create an arena")
    public void create(CommandContext ctx,
                       @Flag(value = "force", shortName = 'f') @Description("Replace an existing arena") boolean force,
                       String name,
                       @Range(min = 1, max = 64) @Default("16") int size) {
        if (arenas.containsKey(name) && !force) {
            ctx.sendFailure(name + " exists. Add -f to replace it.");
            return;
        }
        arenas.put(name, size);
        ctx.sendSuccess("Created " + name + " with " + size + " slots");
    }

    @Subcommand("delete")
    @Confirm
    public void delete(CommandContext ctx, @Suggest(method = "arenaNames") String name) {
        if (arenas.remove(name) == null) {
            ctx.sendFailure("No arena " + name);
            return;
        }
        ctx.sendSuccess("Deleted " + name);
    }

    @Subcommand("list")
    public void list(CommandContext ctx) {
        ctx.sendSuccess("Arenas: " + arenas);
    }

    Iterable<String> arenaNames() {
        return arenas.keySet();
    }
}
