package dev.marulabs.cadence.examples.team

import dev.marulabs.cadence.Command
import dev.marulabs.cadence.argument.ObjectArgument
import dev.marulabs.cadence.argument.PlayerArgument
import dev.marulabs.cadence.kotlin.argument
import dev.marulabs.cadence.kotlin.command
import dev.marulabs.cadence.kotlin.subcommand

/** [FluentTeamCommand] written with the Kotlin DSL. */
fun kotlinTeamCommand(teams: Teams): Command =
    command("team") {
        description("Manage team members")
        execute { ctx -> ctx.sendMessage("Teams: " + teams.byName().keys.joinToString(", ")) }
        argument(ObjectArgument<MutableSet<String>>("team") { teams.byName() }) { team ->
            subcommand("add") {
                description("Add a player to the team")
                argument(PlayerArgument("player")) { player ->
                    execute { ctx ->
                        team.get().add(player.get().name)
                        ctx.sendSuccess("Members: ${team.get()}")
                    }
                }
            }
            subcommand("remove") {
                description("Remove a player from the team")
                argument(PlayerArgument("player")) { player ->
                    execute { ctx ->
                        team.get().remove(player.get().name)
                        ctx.sendSuccess("Members: ${team.get()}")
                    }
                }
            }
        }
    }
