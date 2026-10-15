package dev.marulabs.commandlib.command;

import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Wraps the execution of a command, added with {@code addMiddleware}. A middleware runs after the permission, option,
 * argument and prerequisite checks, only when the input reaches an executor (the generated help skips it), and calls
 * {@link Next#proceed()} to run the remaining middlewares and the executor.
 * <ul>
 * <li>Before {@code proceed()}: checks and context enrichment. Throw {@link CommandPrerequisiteException} to fail the
 * command with its message, or return a stage without calling {@code proceed()} to skip the executor.</li>
 * <li>After {@code proceed()}: chain on the returned stage, such as {@code whenComplete} for a post-execution hook or
 * {@code exceptionally} to map an exception. The stage completes when the executor's work does, also for
 * {@code executeAsync}, and its exception may be wrapped in a {@link java.util.concurrent.CompletionException}.</li>
 * </ul>
 * Middlewares and the executor run on the sync thread, even when a middleware calls {@code proceed()} from another
 * thread. A command's middlewares apply to its argument branches and child commands, outer ones first, unless a child
 * calls {@code disableParentMiddleware()}.
 */
@FunctionalInterface
public interface CommandMiddleware<C extends CommonCommandContext<?, ?>> {
    /**
     * Handles one execution.
     *
     * @return the stage that completes when the execution does, usually {@code next.proceed()} or a stage derived from
     *         it
     * @throws CommandPrerequisiteException to fail the command with the exception's message
     */
    @NotNull
    CompletionStage<?> handle(@NotNull C ctx, @NotNull Next next) throws CommandPrerequisiteException;

    /**
     * The rest of the execution after a middleware.
     */
    @FunctionalInterface
    interface Next {
        /**
         * Runs the remaining middlewares and the executor on the sync thread. It never throws: a failure completes the
         * returned stage exceptionally.
         */
        @NotNull
        CompletableFuture<Void> proceed();
    }
}
