package dev.marulabs.cadence.examples.stats;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.argument.StringArgument;

/**
 * {@link FluentStatsCommand} written with the Java builder.
 */
public final class BuilderStatsCommand {
    private BuilderStatsCommand() {
    }

    public static Command create(StatsStore store) {
        return Command.builder("stats")
                      .description("Show a player's kills")
                      .argument(new StringArgument("player"),
                                branch -> branch.executeAsync((ctx,
                                                               player) -> ctx.supplyAsync(() -> store.kills(player))
                                                                             .thenAccept(kills -> ctx.sendSuccess(player
                                                                                     + " has " + kills + " kills"))))
                      .build();
    }
}
