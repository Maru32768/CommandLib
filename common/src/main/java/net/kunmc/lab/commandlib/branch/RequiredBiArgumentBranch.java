package net.kunmc.lab.commandlib.branch;

import net.kunmc.lab.commandlib.ArgRef;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.CommonArgument;
import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.command.Extractor;
import net.kunmc.lab.commandlib.util.function.TetraConsumer;
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
