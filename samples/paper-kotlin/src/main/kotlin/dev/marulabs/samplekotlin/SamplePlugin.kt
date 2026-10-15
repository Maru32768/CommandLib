package dev.marulabs.samplekotlin

import dev.marulabs.commandlib.CommandLib
import dev.marulabs.commandlib.CommandLibDiagnosticsCommand
import dev.marulabs.commandlib.argument.IntegerArgument
import dev.marulabs.commandlib.argument.PlayerArgument
import dev.marulabs.commandlib.argument.StringArgument
import dev.marulabs.commandlib.kotlin.argument
import dev.marulabs.commandlib.kotlin.command
import dev.marulabs.commandlib.kotlin.flag
import dev.marulabs.commandlib.kotlin.getValue
import dev.marulabs.commandlib.kotlin.subcommand
import org.bukkit.plugin.java.JavaPlugin

class SamplePlugin : JavaPlugin() {
    private val notes = mutableListOf<String>()

    override fun onEnable() {
        CommandLib.register(this, feedCommand(), notesCommand(), CommandLibDiagnosticsCommand("samplekotlin-diagnostics"))
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
