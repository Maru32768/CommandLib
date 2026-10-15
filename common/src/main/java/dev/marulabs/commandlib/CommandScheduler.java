package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * The threads commands run on. The sync executor runs tasks on the thread that may touch the sender and the world, the
 * server thread on Spigot, Paper, Forge and NeoForge, and runs a task at once when it is called on that thread. The
 * async executor runs work off that thread. Each platform's {@code CommandLib} provides one; the testers take one
 * through their builder.
 * <p>
 * The sync executor is chosen per command context, so that a platform with several such threads, such as Folia's
 * region and entity threads, can pick the one owning the sender.
 */
public interface CommandScheduler {
    /**
     * The executor for the thread that owns the sender of {@code ctx}.
     */
    @NotNull
    Executor sync(@NotNull CommonCommandContext<?, ?> ctx);

    /**
     * The executor for work off the sync thread, such as file or database access.
     */
    @NotNull
    Executor async(@NotNull CommonCommandContext<?, ?> ctx);

    /**
     * A scheduler with fixed executors. With {@code Runnable::run} for both, as the testers use by default, an async
     * command completes before its dispatch returns.
     */
    @NotNull
    static CommandScheduler of(@NotNull Executor sync, @NotNull Executor async) {
        Objects.requireNonNull(sync);
        Objects.requireNonNull(async);
        return new CommandScheduler() {
            @Override
            public Executor sync(CommonCommandContext<?, ?> ctx) {
                return sync;
            }

            @Override
            public Executor async(CommonCommandContext<?, ?> ctx) {
                return async;
            }
        };
    }
}
