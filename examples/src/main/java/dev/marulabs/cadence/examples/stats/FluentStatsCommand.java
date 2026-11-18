package dev.marulabs.cadence.examples.stats;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.argument.StringArgument;

/**
 * Use case 4, async I/O: {@code /stats <player>} queries the store on the async executor and replies on the server
 * thread.
 */
public final class FluentStatsCommand extends Command {
    public FluentStatsCommand(StatsStore store) {
        super("stats");
        description("Show a player's kills");

        argument(new StringArgument("player")).executeAsync((ctx, player) -> ctx.supplyAsync(() -> store.kills(player))
                                                                                // Back on the server thread.
                                                                                .thenAccept(kills -> ctx.sendSuccess(player
                                                                                        + " has " + kills + " kills")));
    }
}
