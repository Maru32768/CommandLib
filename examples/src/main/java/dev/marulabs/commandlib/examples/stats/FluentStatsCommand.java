package dev.marulabs.commandlib.examples.stats;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.StringArgument;

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
