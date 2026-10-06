package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.command.CommandExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArgumentBuilder<C extends CommonCommandContext<?, ?>> {
    private final List<CommonArgument<?, C, ?>> arguments = new ArrayList<>();
    private CommandExecutor<C> executor = null;

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

    public void execute(@Nullable CommandExecutor<C> executor) {
        this.executor = executor;
    }

    CommandExecutor<C> executor() {
        return executor;
    }

    List<CommonArgument<?, C, ?>> build() {
        return List.copyOf(arguments);
    }
}
