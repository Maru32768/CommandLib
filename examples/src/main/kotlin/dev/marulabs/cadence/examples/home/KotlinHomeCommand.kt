package dev.marulabs.cadence.examples.home

import dev.marulabs.cadence.Command
import dev.marulabs.cadence.argument.StringArgument
import dev.marulabs.cadence.exception.CommandPrerequisiteException
import dev.marulabs.cadence.kotlin.argument
import dev.marulabs.cadence.kotlin.command
import dev.marulabs.cadence.kotlin.subcommand

/** [FluentHomeCommand] written with the Kotlin DSL. */
fun kotlinHomeCommand(): Command =
    command("home") {
        captions(HomeCaptions.create())
        description(HomeCaptions.DESCRIPTION)
        val homes = mutableSetOf<String>()
        subcommand("go") {
            argument(StringArgument("name")) { name ->
                execute { ctx ->
                    if (name.get() !in homes) throw CommandPrerequisiteException(HomeCaptions.UNKNOWN, mapOf("name" to name.get()))
                    ctx.sendSuccess(HomeCaptions.TELEPORTED, mapOf("name" to name.get()))
                }
            }
        }
        subcommand("set") {
            argument(StringArgument("name")) { name ->
                execute { ctx ->
                    homes += name.get()
                    ctx.sendSuccess(HomeCaptions.SET, mapOf("name" to name.get()))
                }
            }
        }
    }
