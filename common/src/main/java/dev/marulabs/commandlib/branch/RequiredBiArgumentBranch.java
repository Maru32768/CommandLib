package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.ArgRef;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.util.function.TetraConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class RequiredBiArgumentBranch<S, T1, T2, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends RequiredArgumentBranch<S, C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;

    public RequiredBiArgumentBranch(@NotNull Extractor<C, S> extractor,
                                    @NotNull Consumer<CommandExecutor<C>> executeRegistrar,
                                    @NotNull Consumer<Collection<? extends T>> childrenAdder,
                                    @NotNull CommonArgument<T1, C, ?> argument1,
                                    @NotNull CommonArgument<T2, C, ?> argument2) {
        super(extractor, executeRegistrar, childrenAdder);
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    public RequiredBiArgumentBranch<S, T1, T2, C, T> execute(@NotNull TetraConsumer<T1, T2, S, C> action) {
        super.execute((s, ctx) -> action.accept(ctx.getArgument(argument1), ctx.getArgument(argument2), s, ctx));
        return this;
    }

    public RequiredBiArgumentBranch<S, T1, T2, C, T> child(@NotNull BiFunction<ArgRef<T1>, ArgRef<T2>, T> factory) {
        super.child(factory.apply(ArgRef.of(argument1), ArgRef.of(argument2)));
        return this;
    }
}
