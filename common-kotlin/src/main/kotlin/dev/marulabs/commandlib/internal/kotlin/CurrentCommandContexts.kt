package dev.marulabs.commandlib.internal.kotlin

import dev.marulabs.commandlib.CommonCommandContext
import dev.marulabs.commandlib.internal.CurrentCommandContext

// Internal to common-kotlin. Its file class is public on the JVM, so it is in the internal package to stay out of the
// completion of Java code.

/**
 * The command context of the callback running on this thread.
 *
 * @throws IllegalStateException outside of a command callback
 */
internal fun currentCommandContext(): CommonCommandContext<*, *> =
    CurrentCommandContext.get()
        ?: throw IllegalStateException(
            "A command value was read outside of a command callback. Read it inside the callback or from the context: ctx[value].",
        )

/**
 * Runs [block] with [ctx] as the context of this thread's callback.
 */
internal fun runInCommandContext(
    ctx: CommonCommandContext<*, *>,
    block: Runnable,
) {
    CurrentCommandContext.run(ctx, block)
}
