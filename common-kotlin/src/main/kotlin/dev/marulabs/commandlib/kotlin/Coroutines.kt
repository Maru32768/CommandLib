// The coroutine part of the DSL, in a file of its own so that the rest loads without kotlinx-coroutines.
package dev.marulabs.commandlib.kotlin

import dev.marulabs.commandlib.CommonArgument
import dev.marulabs.commandlib.CommonCommand
import dev.marulabs.commandlib.CommonCommandContext
import dev.marulabs.commandlib.branch.ArgumentBranch
import dev.marulabs.commandlib.branch.RequiredArgumentBranch
import dev.marulabs.commandlib.runInCommandContext
import dev.marulabs.commandlib.suggestion.SuggestionBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

/**
 * Dispatches to [executor] and makes [ctx] the current context while the coroutine runs, so that `ref.get()` and the
 * option properties work after the coroutine resumed on another thread.
 */
private class CommandContextDispatcher(
    private val ctx: CommonCommandContext<*, *>,
    private val executor: Executor,
) : CoroutineDispatcher() {
    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        executor.execute { runInCommandContext(ctx, block) }
    }
}

/**
 * The dispatcher of the sync thread, where the sender, the world and the platform API may be used. `executeSuspend`
 * runs its block on it.
 */
val CommonCommandContext<*, *>.syncDispatcher: CoroutineDispatcher
    get() = CommandContextDispatcher(this, syncExecutor())

/**
 * The dispatcher of the async executor, for blocking work such as file or database access:
 * `withContext(ctx.asyncDispatcher) { database.load(id) }`.
 */
val CommonCommandContext<*, *>.asyncDispatcher: CoroutineDispatcher
    get() = CommandContextDispatcher(this, asyncExecutor())

private fun <C : CommonCommandContext<*, *>> C.startCommand(action: suspend CoroutineScope.(C) -> Unit): CompletableFuture<Unit> =
    CoroutineScope(syncDispatcher).future { action(this@startCommand) }

/**
 * Sets a suspending executor, run with [executeAsync][CommonCommand.executeAsync]: the block starts on the sync thread,
 * and the command is done when it returns. An exception it throws is reported like one of a synchronous executor; a
 * `CommandPrerequisiteException` sends its message. Cancelling the coroutine reports nothing.
 *
 * ```kotlin
 * executeSuspend { ctx ->
 *     val stats = withContext(ctx.asyncDispatcher) { database.loadStats(id) }
 *     ctx.sendSuccess("${stats.kills} kills")
 * }
 * ```
 */
fun <C : CommonCommandContext<*, *>> CommonCommand<C, *>.executeSuspend(action: suspend CoroutineScope.(C) -> Unit) {
    executeAsync { ctx -> ctx.startCommand(action) }
}

/**
 * Same as [CommonCommand.executeSuspend] for an argument branch.
 */
fun <C : CommonCommandContext<*, *>, T : CommonCommand<C, T>> ArgumentBranch<C, T>.executeSuspend(
    action: suspend CoroutineScope.(C) -> Unit,
): ArgumentBranch<C, T> = executeAsync { ctx -> ctx.startCommand(action) }

/**
 * Same as [CommonCommand.executeSuspend] for a `require(...)` branch, whose block also receives the extracted value.
 */
fun <S, C : CommonCommandContext<*, *>, T : CommonCommand<C, T>> RequiredArgumentBranch<S, C, T>.executeSuspend(
    action: suspend CoroutineScope.(S, C) -> Unit,
): RequiredArgumentBranch<S, C, T> = executeAsync { value, ctx -> ctx.startCommand { action(value, it) } }

/**
 * Adds a suspending middleware, run like one added with [addMiddleware][CommonCommand.addMiddleware]. It starts on the
 * sync thread and resumes there; calling `next()` runs the remaining middlewares and the executor and returns once
 * they are done, throwing what they threw. Return without calling `next()` to skip the executor, or throw a
 * `CommandPrerequisiteException` to fail the command with its message.
 *
 * ```kotlin
 * addMiddlewareSuspend { ctx, next ->
 *     val allowed = withContext(ctx.asyncDispatcher) { bans.isAllowed(ctx.actor.name) }
 *     if (!allowed) throw CommandPrerequisiteException("You are banned.")
 *     next()
 * }
 * ```
 */
fun <C : CommonCommandContext<*, *>> CommonCommand<C, *>.addMiddlewareSuspend(
    middleware: suspend CoroutineScope.(C, next: suspend () -> Unit) -> Unit,
) {
    addMiddleware { ctx, next -> ctx.startCommand { middleware(it) { next.proceed().await() } } }
}

/**
 * Adds a suspending suggestion provider. It starts on the thread asking for suggestions, may suspend, for example
 * with `withContext(Dispatchers.IO) { ... }`, and the suggestions are sent once it returns. Suggestions may be added
 * from any thread. The provider has no sync thread to return to: do not use the platform API after suspending.
 */
fun <V, C : CommonCommandContext<*, *>, S : CommonArgument<V, C, S>> S.addSuggestionProviderSuspend(
    provider: suspend CoroutineScope.(SuggestionBuilder<C>) -> Unit,
): S =
    addSuggestionProvider { builder ->
        // The context of a suggestion has no scheduler, so the provider resumes on the thread that resumes it.
        val dispatcher = CommandContextDispatcher(builder.context, Runnable::run)
        builder.await(CoroutineScope(dispatcher).future { provider(builder) })
    }
