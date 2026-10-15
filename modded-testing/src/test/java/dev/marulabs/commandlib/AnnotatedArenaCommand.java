package dev.marulabs.commandlib;

import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Confirm;
import dev.marulabs.commandlib.annotation.Cooldown;
import dev.marulabs.commandlib.annotation.Default;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Flag;
import dev.marulabs.commandlib.annotation.Option;
import dev.marulabs.commandlib.annotation.Optional;
import dev.marulabs.commandlib.annotation.Permission;
import dev.marulabs.commandlib.annotation.Range;
import dev.marulabs.commandlib.annotation.Subcommand;
import dev.marulabs.commandlib.annotation.Suggest;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The example command of the annotation API; {@link FluentArenaCommand} is the same command written fluently.
 */
@Command(value = "arena", aliases = "ar")
@Description("Manage arenas")
public final class AnnotatedArenaCommand {
    private final List<String> arenas = new ArrayList<>(List.of("lobby"));

    @Subcommand("create")
    @Description("Create an arena")
    @Permission(defaultPermission = DefaultPermission.ALL, description = "Create arenas")
    public void create(CommandContext ctx,
                       @Flag(value = "force", shortName = 'f') @Description("Replace an existing arena") boolean force,
                       String name,
                       @Range(min = 1, max = 64) @Default("16") int size) {
        arenas.add(name);
        ctx.sendSuccess("Created " + name + " with " + size + " slots" + (force ? " (forced)" : ""));
    }

    @Subcommand("delete")
    @Confirm
    public void delete(CommandContext ctx, @Suggest(method = "arenaNames") String name) {
        arenas.remove(name);
        ctx.sendSuccess("Deleted " + name);
    }

    @Subcommand("invite")
    public void invite(CommandContext ctx, String arena, List<ServerPlayer> players) {
        ctx.sendSuccess("Invited " + players.stream()
                                            .map(x -> x.getName()
                                                       .getString())
                                            .collect(Collectors.joining(", "))
                + " to " + arena);
    }

    @Subcommand("join")
    public void join(CommandContext ctx, String arena, @Optional ServerPlayer player) {
        ctx.sendSuccess(player.getName()
                              .getString()
                + " joined " + arena);
    }

    @Subcommand("list")
    @Cooldown(value = 1, unit = TimeUnit.MINUTES)
    public void list(CommandContext ctx,
                     @Option(value = "limit", shortName = 'n') @Range(min = 1, max = 100) @Default("10") @Description("Maximum count") int limit) {
        ctx.sendSuccess(String.join(", ", arenas.subList(0, Math.min(limit, arenas.size()))));
    }

    @Subcommand("stats")
    public CompletionStage<?> stats(CommandContext ctx, String arena) {
        return ctx.supplyAsync(arena::length)
                  .thenAccept(games -> ctx.sendSuccess(arena + " has " + games + " games"));
    }

    Iterable<String> arenaNames() {
        return arenas;
    }
}
