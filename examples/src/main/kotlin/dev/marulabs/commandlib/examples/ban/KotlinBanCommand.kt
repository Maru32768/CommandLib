package dev.marulabs.commandlib.examples.ban

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.CommandContext
import dev.marulabs.commandlib.DefaultPermission
import dev.marulabs.commandlib.Options
import dev.marulabs.commandlib.argument.PlayerArgument
import dev.marulabs.commandlib.kotlin.argument
import dev.marulabs.commandlib.kotlin.command
import dev.marulabs.commandlib.kotlin.flag
import dev.marulabs.commandlib.kotlin.getValue

/** [FluentBanCommand] written with the Kotlin DSL. */
fun kotlinBanCommand(): Command =
    command("ban") {
        description("Ban a player")
        permission(DefaultPermission.OP, "Ban players")
        val silent by flag("silent", 's', "Do not announce the ban")
        val reason by option(Options.string<CommandContext>("reason", 'r', "No reason").description("Why the player is banned"))
        argument(PlayerArgument("target")) { target ->
            execute { ctx -> ctx.sendSuccess("Banned ${target.get().name}: $reason" + if (silent) " (silent)" else "") }
        }
    }
