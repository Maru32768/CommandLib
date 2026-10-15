package dev.marulabs.commandlib;

import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArgumentBuilder<C extends CommonCommandContext<?, ?>> {
    private final List<CommonArgument<?, C, ?>> arguments = new ArrayList<>();
    // Null until execute is called.
    private @Nullable AsyncCommandExecutor<C> executor = null;

    public ArgumentBuilder<C> argument(@NotNull CommonArgument<?, C, ?> argument) {
        Objects.requireNonNull(argument);
        boolean duplicated = arguments.stream()
                                      .anyMatch(x -> x.name()
                                                      .equals(argument.name()));
        if (duplicated) {
            throw new IllegalArgumentException("Duplicate argument name: " + argument.name());
        }
        arguments.add(argument);
        return this;
    }

    public void execute(@NotNull CommandExecutor<C> executor) {
        executeAsync(AsyncCommandExecutor.of(executor));
    }

    /**
     * Sets an executor whose work finishes later, like {@link CommonCommand#executeAsync(AsyncCommandExecutor)}.
     */
    public void executeAsync(@NotNull AsyncCommandExecutor<C> executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    @Nullable
    AsyncCommandExecutor<C> executor() {
        return executor;
    }

    List<CommonArgument<?, C, ?>> build() {
        return List.copyOf(arguments);
    }
}
