package dev.marulabs.cadence.examples.score;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.argument.StringArgument;
import dev.marulabs.cadence.exception.CommandPrerequisiteException;

import java.util.Map;

/**
 * Use case 7, a result for vanilla: {@code /execute store result score ... run score <player>} stores the player's
 * score, and an unknown player fails the command, which stores 0.
 */
public final class FluentScoreCommand extends Command {
    public static final Map<String, Integer> SCORES = Map.of("Steve", 42, "Alex", 7);

    public FluentScoreCommand() {
        super("score");
        description("Look up a player's score");

        StringArgument player = new StringArgument("player");
        argument(player).execute(ctx -> {
            Integer score = SCORES.get(ctx.getArgument(player));
            if (score == null) {
                throw new CommandPrerequisiteException("No score for " + ctx.getArgument(player));
            }
            ctx.setResult(score);
            ctx.sendSuccess(ctx.getArgument(player) + " has " + score + " points");
        });
    }
}
