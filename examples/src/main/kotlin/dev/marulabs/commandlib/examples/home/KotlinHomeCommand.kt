package dev.marulabs.commandlib.examples.home

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.argument.StringArgument
import dev.marulabs.commandlib.exception.CommandPrerequisiteException
import dev.marulabs.commandlib.kotlin.argument
import dev.marulabs.commandlib.kotlin.command
import dev.marulabs.commandlib.kotlin.subcommand

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
