package dev.marulabs.commandlib

// shortcut: reaches the package-private CurrentCommandContext from the same package, so common-kotlin must be loaded by
// the class loader of common, as it is when both are shaded into one plugin; make CurrentCommandContext public if
// they ever need separate loaders.

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
