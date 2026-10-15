package dev.marulabs.commandlib.kotlin

import dev.marulabs.commandlib.CommonCommand
import dev.marulabs.commandlib.CommonCommandContext
import dev.marulabs.commandlib.command.Extractor

/**
 * Provides the value of type [V], the type [provider] returns, to the executions of this command, its argument
 * branches and its child commands, as `provide(V::class.java) { ctx -> ... }` does:
 *
 * ```kotlin
 * command("team") {
 *     provide { ctx -> games.of(ctx.player) }    // a GamePlayer
 *     subcommand("show") { execute { ctx -> ctx.sendMessage(ctx.get<GamePlayer>().team) } }
 * }
 * ```
 *
 * Give the type explicitly with `provide(GamePlayer::class.java) { ... }` when the lambda returns a subtype.
 */
inline fun <C : CommonCommandContext<*, *>, reified V : Any> CommonCommand<C, *>.provide(noinline provider: (C) -> V) {
    provide(V::class.java, Extractor<C, V> { provider(it) })
}

/**
 * The value of type [T]: the one a middleware put, or else the one the command provides.
 *
 * @throws IllegalStateException if nothing provides or put the value
 */
inline fun <reified T : Any> CommonCommandContext<*, *>.get(): T = get(T::class.java)

/**
 * The value of type [T], or null when nothing provides or put it.
 */
inline fun <reified T : Any> CommonCommandContext<*, *>.getOrNull(): T? = find(T::class.java).orElse(null)

/**
 * Sets the value of type [T] for the rest of the execution, for the later middlewares and the executor.
 */
inline fun <reified T : Any> CommonCommandContext<*, *>.put(value: T) {
    put(T::class.java, value)
}
