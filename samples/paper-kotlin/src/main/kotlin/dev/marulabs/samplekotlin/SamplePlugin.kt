package dev.marulabs.samplekotlin

import dev.marulabs.cadence.Cadence
import dev.marulabs.cadence.CadenceDiagnosticsCommand
import dev.marulabs.cadence.argument.IntegerArgument
import dev.marulabs.cadence.argument.PlayerArgument
import dev.marulabs.cadence.argument.StringArgument
import dev.marulabs.cadence.kotlin.argument
import dev.marulabs.cadence.kotlin.command
import dev.marulabs.cadence.kotlin.flag
import dev.marulabs.cadence.kotlin.getValue
import dev.marulabs.cadence.kotlin.subcommand
import org.bukkit.plugin.java.JavaPlugin

class SamplePlugin : JavaPlugin() {
    private val notes = mutableListOf<String>()

    override fun onEnable() {
        Cadence.register(this, feedCommand(), notesCommand(), CadenceDiagnosticsCommand("samplekotlin-diagnostics"))
    }

    /**
     * /feed [-s|--silent] <targets...>
     */
    private fun feedCommand() = command("feed") {
        description("Fill the hunger bars of players")
        val silent by flag("silent", 's', "Do not report the result")
        argument(PlayerArgument("targets").variadic()) { targets ->
            execute { ctx ->
                targets.get().forEach { it.foodLevel = 20 }
                if (!silent) ctx.sendSuccess("Fed ${targets.get().size} players")
            }
        }
    }

    /**
     * /notes add <text>
     * /notes list [page]
     */
    private fun notesCommand() = command("notes") {
        description("Keep notes")
        subcommand("add") {
            argument(StringArgument("text", StringArgument.Type.PHRASE)) { text ->
                execute { ctx ->
                    notes += text.get()
                    ctx.sendSuccess("Added note #${notes.size}")
                }
            }
        }
        subcommand("list") {
            argument(IntegerArgument("page", 1, 100).optional(1)) { page ->
                execute { ctx ->
                    val shown = notes.drop((page.get() - 1) * 5).take(5)
                    ctx.sendSuccess(if (shown.isEmpty()) "No notes" else shown.joinToString("\n"))
                }
            }
        }
    }
}
