package dev.marulabs.commandlib.examples.score;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Execute;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

import static dev.marulabs.commandlib.examples.score.FluentScoreCommand.SCORES;

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
