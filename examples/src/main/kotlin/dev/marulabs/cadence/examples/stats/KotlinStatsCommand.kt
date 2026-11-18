package dev.marulabs.cadence.examples.stats

import dev.marulabs.cadence.Command
import dev.marulabs.cadence.argument.StringArgument
import dev.marulabs.cadence.kotlin.argument
import dev.marulabs.cadence.kotlin.asyncDispatcher
import dev.marulabs.cadence.kotlin.command
import dev.marulabs.cadence.kotlin.executeSuspend
import kotlinx.coroutines.withContext

/** [FluentStatsCommand] written with the Kotlin DSL: the coroutine resumes on the server thread. */
fun kotlinStatsCommand(store: StatsStore): Command =
    command("stats") {
        description("Show a player's kills")
        argument(StringArgument("player")) { player ->
            executeSuspend { ctx ->
                val kills = withContext(ctx.asyncDispatcher) { store.kills(player.get()) }
                ctx.sendSuccess("${player.get()} has $kills kills")
            }
        }
    }
