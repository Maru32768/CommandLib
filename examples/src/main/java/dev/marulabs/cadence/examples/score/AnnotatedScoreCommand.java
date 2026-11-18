package dev.marulabs.cadence.examples.score;

import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.annotation.Command;
import dev.marulabs.cadence.annotation.Description;
import dev.marulabs.cadence.annotation.Execute;
import dev.marulabs.cadence.exception.CommandPrerequisiteException;

import static dev.marulabs.cadence.examples.score.FluentScoreCommand.SCORES;

/**
 * {@link FluentScoreCommand} written with annotations: the {@code int} the method returns is the result.
 */
@Command("score")
@Description("Look up a player's score")
public final class AnnotatedScoreCommand {
    @Execute
    public int score(CommandContext ctx, String player) throws CommandPrerequisiteException {
        Integer score = SCORES.get(player);
        if (score == null) {
            throw new CommandPrerequisiteException("No score for " + player);
        }
        ctx.sendSuccess(player + " has " + score + " points");
        return score;
    }
}
