package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.ArgRef;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.util.function.TriConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public final class RequiredUnaryArgumentBranch<S, T1, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends RequiredArgumentBranch<S, C, T> {
    private final CommonArgument<T1, C, ?> argument1;

    public RequiredUnaryArgumentBranch(@NotNull Extractor<C, S> extractor,
                                       @NotNull Consumer<CommandExecutor<C>> executeRegistrar,
                                       @NotNull Consumer<Collection<? extends T>> childrenAdder,
                                       @NotNull CommonArgument<T1, C, ?> argument1) {
        super(extractor, executeRegistrar, childrenAdder);
        this.argument1 = argument1;
    }

    public RequiredUnaryArgumentBranch<S, T1, C, T> execute(@NotNull TriConsumer<T1, S, C> action) {
        super.execute((s, ctx) -> action.accept(ctx.getArgument(argument1), s, ctx));
        return this;
    }

    public RequiredUnaryArgumentBranch<S, T1, C, T> child(@NotNull Function<ArgRef<T1>, T> factory) {
        super.child(factory.apply(ArgRef.of(argument1)));
        return this;
    }
}
