package dev.marulabs.cadence.examples.stats;

import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.annotation.Command;
import dev.marulabs.cadence.annotation.Description;
import dev.marulabs.cadence.annotation.Execute;

import java.util.concurrent.CompletionStage;

/**
 * {@link FluentStatsCommand} written with annotations: a method that returns a {@code CompletionStage} runs as
 * {@code executeAsync}.
 */
@Command("stats")
@Description("Show a player's kills")
public final class AnnotatedStatsCommand {
    private final StatsStore store;

    public AnnotatedStatsCommand(StatsStore store) {
        this.store = store;
    }

    @Execute
    public CompletionStage<?> stats(CommandContext ctx, String player) {
        return ctx.supplyAsync(() -> store.kills(player))
                  .thenAccept(kills -> ctx.sendSuccess(player + " has " + kills + " kills"));
    }
}
