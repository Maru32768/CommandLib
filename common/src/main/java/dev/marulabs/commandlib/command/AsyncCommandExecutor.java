package dev.marulabs.commandlib.command;

import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * An executor whose work finishes later, set with {@code executeAsync}. Like {@link CommandExecutor} it is called on
 * the sync thread; it starts the work and returns the stage that completes when the command is done. A stage that fails
 * reports its exception the way a synchronous executor's exception is reported.
 */
@FunctionalInterface
public interface AsyncCommandExecutor<C extends CommonCommandContext<?, ?>> {
    @NotNull
    CompletionStage<?> apply(@NotNull C ctx) throws CommandPrerequisiteException;

    /**
     * Adapts a synchronous executor: the result runs {@code executor} and returns a completed stage.
     */
    @NotNull
    static <C extends CommonCommandContext<?, ?>> AsyncCommandExecutor<C> of(@NotNull CommandExecutor<C> executor) {
        Objects.requireNonNull(executor);
        return ctx -> {
            executor.accept(ctx);
            return CompletableFuture.completedFuture(null);
        };
    }
}
