package dev.marulabs.commandlib.examples.kit

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.argument.IntegerArgument
import dev.marulabs.commandlib.argument.PlayerArgument
import dev.marulabs.commandlib.argument.StringArgument
import dev.marulabs.commandlib.kotlin.argument
import dev.marulabs.commandlib.kotlin.command
import dev.marulabs.commandlib.kotlin.subcommand

/** [FluentKitCommand] written with the Kotlin DSL. */
fun kotlinKitCommand(): Command =
    command("kit") {
        description("Hand out kits")
        subcommand("give") {
            description("Give yourself a kit")
            argument(StringArgument("name"), IntegerArgument("count", 1, 64).optional(1)) { name, count ->
                execute { ctx -> ctx.sendSuccess("Gave ${count.get()} x ${name.get()}") }
            }
        }
        subcommand("send") {
            description("Send the starter kit to players")
            argument(PlayerArgument("players").variadic()) { players ->
                execute { ctx -> ctx.sendSuccess("Sent the starter kit to " + players.get().joinToString(", ") { it.name }) }
            }
        }
    }
