package dev.marulabs.commandlib.examples.stats

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.argument.StringArgument
import dev.marulabs.commandlib.kotlin.argument
import dev.marulabs.commandlib.kotlin.asyncDispatcher
import dev.marulabs.commandlib.kotlin.command
import dev.marulabs.commandlib.kotlin.executeSuspend
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
