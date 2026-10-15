// Compiled by each <platform>-kotlin module against its platform's Command, which every platform names
// dev.marulabs.commandlib.Command.
package dev.marulabs.commandlib.kotlin

import dev.marulabs.commandlib.Command
import dev.marulabs.commandlib.CommandContext
import dev.marulabs.commandlib.branch.ArgumentBranch
import dev.marulabs.commandlib.branch.RequiredArgumentBranch

/**
 * Creates a command and configures it in [block], whose receiver is the command:
 *
 * ```kotlin
 * val heal = command("heal") {
 *     description("Heal a player")
 *     val silent by flag("silent", 's')
 *     argument(PlayerArgument("target")) { target ->
 *         execute { ctx -> if (!silent) ctx.sendSuccess("Healed ${target.get().name}") }
 *     }
 *     subcommand("all") { execute { ctx -> ctx.sendSuccess("Healed everyone") } }
 * }
 * ```
 */
fun command(
    name: String,
    block: CommandBlock<Command>,
): Command = Command(name).apply(block)

/**
 * Adds a child command, `/<parent> <name>`.
 */
fun Command.subcommand(
    name: String,
    block: CommandBlock<Command>,
): Command = command(name, block).also { addChildren(it) }

/**
 * Adds a child command after the arguments of this branch, `/<parent> <arguments> <name>`. Its callbacks read the
 * arguments of the branch through the references the branch block received.
 */
fun ArgumentBranch<CommandContext, Command>.subcommand(
    name: String,
    block: CommandBlock<Command>,
): Command = command(name, block).also { child(it) }

/**
 * Adds a child command to a `require(...)` branch, `/<parent> <name>`, which runs only when the branch's extractor gives
 * a value.
 */
fun <S> RequiredArgumentBranch<S, CommandContext, Command>.subcommand(
    name: String,
    block: CommandBlock<Command>,
): Command = command(name, block).also { child(it) }
