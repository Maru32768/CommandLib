package dev.marulabs.cadence.examples.score;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.argument.StringArgument;
import dev.marulabs.cadence.exception.CommandPrerequisiteException;

import static dev.marulabs.cadence.examples.score.FluentScoreCommand.SCORES;

/**
 * {@link FluentScoreCommand} written with the Java builder.
 */
public final class BuilderScoreCommand {
    private BuilderScoreCommand() {
    }

    public static Command create() {
        StringArgument player = new StringArgument("player");
        return Command.builder("score")
                      .description("Look up a player's score")
                      .argument(player, branch -> branch.execute(ctx -> {
                          Integer score = SCORES.get(ctx.getArgument(player));
                          if (score == null) {
                              throw new CommandPrerequisiteException("No score for " + ctx.getArgument(player));
                          }
                          ctx.setResult(score);
                          ctx.sendSuccess(ctx.getArgument(player) + " has " + score + " points");
                      }))
                      .build();
    }
}
