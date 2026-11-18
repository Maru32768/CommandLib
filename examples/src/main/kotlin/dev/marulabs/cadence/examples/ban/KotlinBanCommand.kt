package dev.marulabs.cadence.examples.ban

import dev.marulabs.cadence.Command
import dev.marulabs.cadence.CommandContext
import dev.marulabs.cadence.DefaultPermission
import dev.marulabs.cadence.Options
import dev.marulabs.cadence.argument.PlayerArgument
import dev.marulabs.cadence.kotlin.argument
import dev.marulabs.cadence.kotlin.command
import dev.marulabs.cadence.kotlin.flag
import dev.marulabs.cadence.kotlin.getValue

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
