package dev.marulabs.cadence.branch;

import dev.marulabs.cadence.ArgRef;
import dev.marulabs.cadence.CommonArgument;
import dev.marulabs.cadence.CommonCommand;
import dev.marulabs.cadence.CommonCommandContext;
import dev.marulabs.cadence.command.AsyncCommandExecutor;
import dev.marulabs.cadence.command.Extractor;
import dev.marulabs.cadence.util.function.TriConsumer;
import dev.marulabs.cadence.util.function.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public final class RequiredUnaryArgumentBranch<S, T1, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends RequiredArgumentBranch<S, C, T> {
    private final CommonArgument<T1, C, ?> argument1;

    public RequiredUnaryArgumentBranch(@NotNull Extractor<C, S> extractor,
                                       @NotNull Consumer<AsyncCommandExecutor<C>> executeRegistrar,
                                       @NotNull Consumer<Collection<? extends T>> childrenAdder,
                                       @NotNull CommonArgument<T1, C, ?> argument1) {
        super(extractor, executeRegistrar, childrenAdder);
        this.argument1 = argument1;
    }

    public RequiredUnaryArgumentBranch<S, T1, C, T> execute(@NotNull TriConsumer<C, T1, S> action) {
        super.execute((ctx, s) -> action.accept(ctx, ctx.getArgument(argument1), s));
        return this;
    }

    public RequiredUnaryArgumentBranch<S, T1, C, T> executeAsync(@NotNull TriFunction<C, T1, S, CompletionStage<?>> action) {
        super.executeAsync((ctx, s) -> action.apply(ctx, ctx.getArgument(argument1), s));
        return this;
    }

    public RequiredUnaryArgumentBranch<S, T1, C, T> child(@NotNull Function<ArgRef<T1>, T> factory) {
        super.child(factory.apply(ArgRef.of(argument1)));
        return this;
    }
}
