package dev.marulabs.commandlib.kotlin

import dev.marulabs.commandlib.ArgRef
import dev.marulabs.commandlib.CommandOption
import dev.marulabs.commandlib.CommonArgument
import dev.marulabs.commandlib.CommonCommand
import dev.marulabs.commandlib.CommonCommandContext
import dev.marulabs.commandlib.Options
import dev.marulabs.commandlib.branch.ArgumentBranch
import dev.marulabs.commandlib.branch.RequiredArgumentBranch
import dev.marulabs.commandlib.command.Extractor
import dev.marulabs.commandlib.internal.kotlin.currentCommandContext
import kotlin.reflect.KProperty

/**
 * Marks the receivers of the DSL blocks, so that a block cannot call the outer command by accident: inside
 * `argument(...) { }`, `description(...)` sets the branch's description, never the command's.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE)
annotation class CommandLibDsl

/**
 * The block of a command: the command itself is the receiver, so every method of the fluent API, such as
 * `permission(...)`, `addPrerequisite(...)` or `cooldown(...)`, can be called in it.
 */
typealias CommandBlock<T> = (@CommandLibDsl T).() -> Unit

/**
 * Adds an argument branch and configures it in [block], which receives a reference to each argument. Read a value in a
 * callback with `target.get()`, or with `ctx[target]` where no callback runs on the thread, such as in
 * work started on another thread.
 *
 * ```kotlin
 * argument(PlayerArgument("target")) { target ->
 *     execute { ctx -> ctx.sendMessage("Healed ${target.get().name}") }
 * }
 * ```
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>, A1> T.argument(
    argument1: CommonArgument<A1, C, *>,
    block: (@CommandLibDsl ArgumentBranch<C, T>).(ArgRef<A1>) -> Unit,
): ArgumentBranch<C, T> = branch(argument1).apply { block(ArgRef.of(argument1)) }

/**
 * Same as the one-argument [argument] with two arguments.
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>, A1, A2> T.argument(
    argument1: CommonArgument<A1, C, *>,
    argument2: CommonArgument<A2, C, *>,
    block: (@CommandLibDsl ArgumentBranch<C, T>).(ArgRef<A1>, ArgRef<A2>) -> Unit,
): ArgumentBranch<C, T> = branch(argument1, argument2).apply { block(ArgRef.of(argument1), ArgRef.of(argument2)) }

/**
 * Same as the one-argument [argument] with three arguments.
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>, A1, A2, A3> T.argument(
    argument1: CommonArgument<A1, C, *>,
    argument2: CommonArgument<A2, C, *>,
    argument3: CommonArgument<A3, C, *>,
    block: (@CommandLibDsl ArgumentBranch<C, T>).(ArgRef<A1>, ArgRef<A2>, ArgRef<A3>) -> Unit,
): ArgumentBranch<C, T> =
    branch(argument1, argument2, argument3).apply {
        block(ArgRef.of(argument1), ArgRef.of(argument2), ArgRef.of(argument3))
    }

/**
 * Same as the one-argument [argument] with four arguments.
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>, A1, A2, A3, A4> T.argument(
    argument1: CommonArgument<A1, C, *>,
    argument2: CommonArgument<A2, C, *>,
    argument3: CommonArgument<A3, C, *>,
    argument4: CommonArgument<A4, C, *>,
    block: (@CommandLibDsl ArgumentBranch<C, T>).(ArgRef<A1>, ArgRef<A2>, ArgRef<A3>, ArgRef<A4>) -> Unit,
): ArgumentBranch<C, T> =
    branch(argument1, argument2, argument3, argument4).apply {
        block(ArgRef.of(argument1), ArgRef.of(argument2), ArgRef.of(argument3), ArgRef.of(argument4))
    }

/**
 * Same as the one-argument [argument] with five arguments.
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>, A1, A2, A3, A4, A5> T.argument(
    argument1: CommonArgument<A1, C, *>,
    argument2: CommonArgument<A2, C, *>,
    argument3: CommonArgument<A3, C, *>,
    argument4: CommonArgument<A4, C, *>,
    argument5: CommonArgument<A5, C, *>,
    block: (@CommandLibDsl ArgumentBranch<C, T>).(ArgRef<A1>, ArgRef<A2>, ArgRef<A3>, ArgRef<A4>, ArgRef<A5>) -> Unit,
): ArgumentBranch<C, T> =
    branch(argument1, argument2, argument3, argument4, argument5).apply {
        block(
            ArgRef.of(argument1),
            ArgRef.of(argument2),
            ArgRef.of(argument3),
            ArgRef.of(argument4),
            ArgRef.of(argument5),
        )
    }

/**
 * Same as the one-argument [argument] with six arguments.
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>, A1, A2, A3, A4, A5, A6> T.argument(
    argument1: CommonArgument<A1, C, *>,
    argument2: CommonArgument<A2, C, *>,
    argument3: CommonArgument<A3, C, *>,
    argument4: CommonArgument<A4, C, *>,
    argument5: CommonArgument<A5, C, *>,
    argument6: CommonArgument<A6, C, *>,
    block: (@CommandLibDsl ArgumentBranch<C, T>).(
        ArgRef<A1>,
        ArgRef<A2>,
        ArgRef<A3>,
        ArgRef<A4>,
        ArgRef<A5>,
        ArgRef<A6>,
    ) -> Unit,
): ArgumentBranch<C, T> =
    branch(argument1, argument2, argument3, argument4, argument5, argument6).apply {
        block(
            ArgRef.of(argument1),
            ArgRef.of(argument2),
            ArgRef.of(argument3),
            ArgRef.of(argument4),
            ArgRef.of(argument5),
            ArgRef.of(argument6),
        )
    }

/**
 * Same as the one-argument [argument] with seven arguments.
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>, A1, A2, A3, A4, A5, A6, A7> T.argument(
    argument1: CommonArgument<A1, C, *>,
    argument2: CommonArgument<A2, C, *>,
    argument3: CommonArgument<A3, C, *>,
    argument4: CommonArgument<A4, C, *>,
    argument5: CommonArgument<A5, C, *>,
    argument6: CommonArgument<A6, C, *>,
    argument7: CommonArgument<A7, C, *>,
    block: (@CommandLibDsl ArgumentBranch<C, T>).(
        ArgRef<A1>,
        ArgRef<A2>,
        ArgRef<A3>,
        ArgRef<A4>,
        ArgRef<A5>,
        ArgRef<A6>,
        ArgRef<A7>,
    ) -> Unit,
): ArgumentBranch<C, T> =
    branch(argument1, argument2, argument3, argument4, argument5, argument6, argument7).apply {
        block(
            ArgRef.of(argument1),
            ArgRef.of(argument2),
            ArgRef.of(argument3),
            ArgRef.of(argument4),
            ArgRef.of(argument5),
            ArgRef.of(argument6),
            ArgRef.of(argument7),
        )
    }

/**
 * Adds an argument branch of the arguments in [arguments], such as eight or more, and configures it in [block]. Keep
 * the arguments in variables and read each value typed from its argument with `ctx[argument]`:
 *
 * ```kotlin
 * val world = WorldArgument("world")
 * arguments(listOf(x1, y1, z1, x2, y2, z2, world, name)) {
 *     execute { ctx -> ctx.sendMessage("${ctx[name]} in ${ctx[world].name}") }
 * }
 * ```
 *
 * It is named apart from [argument] so that `argument(a) { ... }` stays the typed form.
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>> T.arguments(
    arguments: List<CommonArgument<*, C, *>>,
    block: (@CommandLibDsl ArgumentBranch<C, T>).() -> Unit,
): ArgumentBranch<C, T> = argument(arguments).apply(block)

// The list form adds the same branch as the typed argument(...) overloads of the fluent API.
private fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>> T.branch(
    vararg arguments: CommonArgument<*, C, *>,
): ArgumentBranch<C, T> = argument(arguments.toList())

/**
 * Configures [CommonCommand.require] in [block]. Its executor is the command's own, which runs only when [extractor]
 * gives a value and receives the value; a `subcommand` of it is checked by the extractor too, and reads the value
 * again from its context:
 *
 * ```kotlin
 * require(Command.player()) {
 *     execute { player, ctx -> player.health = 20.0 }
 *     subcommand("food") { execute { ctx -> ctx.player.foodLevel = 20 } }
 * }
 * ```
 */
fun <S, C : CommonCommandContext<*, *>, T : CommonCommand<C, T>> T.require(
    extractor: Extractor<C, S>,
    block: (@CommandLibDsl RequiredArgumentBranch<S, C, T>).() -> Unit,
): RequiredArgumentBranch<S, C, T> = require(extractor).apply(block)

/**
 * Same as [CommonCommand.require] with a block, after the arguments of this branch.
 */
fun <S, C : CommonCommandContext<*, *>, T : CommonCommand<C, T>> ArgumentBranch<C, T>.require(
    extractor: Extractor<C, S>,
    block: (@CommandLibDsl RequiredArgumentBranch<S, C, T>).() -> Unit,
): RequiredArgumentBranch<S, C, T> = require(extractor).apply(block)

/**
 * The value of [ref] in this context.
 */
operator fun <V> CommonCommandContext<*, *>.get(ref: ArgRef<V>): V = ref.get(this)

/**
 * The value of [argument] in this context, also of an argument of a parent branch.
 */
operator fun <V> CommonCommandContext<*, *>.get(argument: CommonArgument<V, *, *>): V = getArgument(argument)

/**
 * The value of [option] in this context, its default when it was not given.
 */
operator fun <V> CommonCommandContext<*, *>.get(option: CommandOption<V, *>): V = getOption(option)

/**
 * Reads a registered option as a delegated property, from the command callback running on this thread:
 *
 * ```kotlin
 * val limit by option(Options.integer("limit", 'n', 10, 1, 100))
 * execute { ctx -> ctx.sendMessage("limit $limit") }
 * ```
 *
 * Use `ctx[option]` where no callback is running.
 *
 * @throws IllegalStateException when read outside of a command callback
 */
operator fun <V> CommandOption<V, *>.getValue(
    thisRef: Any?,
    property: KProperty<*>,
): V = currentCommandContext().getOption(this)

/**
 * Registers a flag such as `--force` / `-f`, `false` unless given. Read it as a delegated property:
 * `val force by flag("force", 'f')`.
 */
fun <C : CommonCommandContext<*, *>> CommonCommand<C, *>.flag(
    name: String,
    shortName: Char? = null,
    description: String? = null,
): CommandOption<Boolean, C> {
    val flag: CommandOption<Boolean, C> = if (shortName == null) Options.flag(name) else Options.flag(name, shortName)
    description?.let { flag.description(it) }
    return option(flag)
}
