package dev.marulabs.cadence.examples.score

import dev.marulabs.cadence.Command
import dev.marulabs.cadence.argument.StringArgument
import dev.marulabs.cadence.examples.score.FluentScoreCommand.SCORES
import dev.marulabs.cadence.exception.CommandPrerequisiteException
import dev.marulabs.cadence.kotlin.argument
import dev.marulabs.cadence.kotlin.command

/** [FluentScoreCommand] written with the Kotlin DSL. */
fun kotlinScoreCommand(): Command =
    command("score") {
        description("Look up a player's score")
        argument(StringArgument("player")) { player ->
            execute { ctx ->
                val score = SCORES[player.get()] ?: throw CommandPrerequisiteException("No score for ${player.get()}")
                ctx.setResult(score)
                ctx.sendSuccess("${player.get()} has $score points")
            }
        }
    }
