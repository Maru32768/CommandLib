package dev.marulabs.commandlib.examples.score

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.argument.StringArgument
import dev.marulabs.commandlib.examples.score.FluentScoreCommand.SCORES
import dev.marulabs.commandlib.exception.CommandPrerequisiteException
import dev.marulabs.commandlib.kotlin.argument
import dev.marulabs.commandlib.kotlin.command

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
