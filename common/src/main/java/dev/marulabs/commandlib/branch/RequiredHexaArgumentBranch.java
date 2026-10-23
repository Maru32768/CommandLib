package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.ArgRef;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.util.function.HexaFunction;
import dev.marulabs.commandlib.util.function.OctoConsumer;
import dev.marulabs.commandlib.util.function.OctoFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class RequiredHexaArgumentBranch<S, T1, T2, T3, T4, T5, T6, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends RequiredArgumentBranch<S, C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;
    private final CommonArgument<T3, C, ?> argument3;
    private final CommonArgument<T4, C, ?> argument4;
    private final CommonArgument<T5, C, ?> argument5;
    private final CommonArgument<T6, C, ?> argument6;

    public RequiredHexaArgumentBranch(@NotNull Extractor<C, S> extractor,
                                      @NotNull Consumer<AsyncCommandExecutor<C>> executeRegistrar,
                                      @NotNull Consumer<Collection<? extends T>> childrenAdder,
                                      @NotNull CommonArgument<T1, C, ?> argument1,
                                      @NotNull CommonArgument<T2, C, ?> argument2,
                                      @NotNull CommonArgument<T3, C, ?> argument3,
                                      @NotNull CommonArgument<T4, C, ?> argument4,
                                      @NotNull CommonArgument<T5, C, ?> argument5,
                                      @NotNull CommonArgument<T6, C, ?> argument6) {
        super(extractor, executeRegistrar, childrenAdder);
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
        this.argument4 = argument4;
        this.argument5 = argument5;
        this.argument6 = argument6;
    }

    public RequiredHexaArgumentBranch<S, T1, T2, T3, T4, T5, T6, C, T> execute(@NotNull OctoConsumer<C, T1, T2, T3, T4, T5, T6, S> action) {
        super.execute((ctx, s) -> action.accept(ctx,
                                                ctx.getArgument(argument1),
                                                ctx.getArgument(argument2),
                                                ctx.getArgument(argument3),
                                                ctx.getArgument(argument4),
                                                ctx.getArgument(argument5),
                                                ctx.getArgument(argument6),
                                                s));
        return this;
    }

    public RequiredHexaArgumentBranch<S, T1, T2, T3, T4, T5, T6, C, T> executeAsync(@NotNull OctoFunction<C, T1, T2, T3, T4, T5, T6, S, CompletionStage<?>> action) {
        super.executeAsync((ctx, s) -> action.apply(ctx,
                                                    ctx.getArgument(argument1),
                                                    ctx.getArgument(argument2),
                                                    ctx.getArgument(argument3),
                                                    ctx.getArgument(argument4),
                                                    ctx.getArgument(argument5),
                                                    ctx.getArgument(argument6),
                                                    s));
        return this;
    }

    public RequiredHexaArgumentBranch<S, T1, T2, T3, T4, T5, T6, C, T> child(@NotNull HexaFunction<ArgRef<T1>, ArgRef<T2>, ArgRef<T3>, ArgRef<T4>, ArgRef<T5>, ArgRef<T6>, T> factory) {
        super.child(factory.apply(ArgRef.of(argument1),
                                  ArgRef.of(argument2),
                                  ArgRef.of(argument3),
                                  ArgRef.of(argument4),
                                  ArgRef.of(argument5),
                                  ArgRef.of(argument6)));
        return this;
    }
}
