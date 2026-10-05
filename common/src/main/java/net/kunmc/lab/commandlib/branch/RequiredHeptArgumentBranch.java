package net.kunmc.lab.commandlib.branch;

import net.kunmc.lab.commandlib.ArgRef;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.CommonArgument;
import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.command.Extractor;
import net.kunmc.lab.commandlib.util.function.HeptFunction;
import net.kunmc.lab.commandlib.util.function.NonaConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

public final class RequiredHeptArgumentBranch<S, T1, T2, T3, T4, T5, T6, T7, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends RequiredArgumentBranch<S, C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;
    private final CommonArgument<T3, C, ?> argument3;
    private final CommonArgument<T4, C, ?> argument4;
    private final CommonArgument<T5, C, ?> argument5;
    private final CommonArgument<T6, C, ?> argument6;
    private final CommonArgument<T7, C, ?> argument7;

    public RequiredHeptArgumentBranch(@NotNull Extractor<C, S> extractor,
                                      @NotNull Consumer<CommandExecutor<C>> executeRegistrar,
                                      @NotNull Consumer<Collection<? extends T>> childrenAdder,
                                      @NotNull CommonArgument<T1, C, ?> argument1,
                                      @NotNull CommonArgument<T2, C, ?> argument2,
                                      @NotNull CommonArgument<T3, C, ?> argument3,
                                      @NotNull CommonArgument<T4, C, ?> argument4,
                                      @NotNull CommonArgument<T5, C, ?> argument5,
                                      @NotNull CommonArgument<T6, C, ?> argument6,
                                      @NotNull CommonArgument<T7, C, ?> argument7) {
        super(extractor, executeRegistrar, childrenAdder);
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
        this.argument4 = argument4;
        this.argument5 = argument5;
        this.argument6 = argument6;
        this.argument7 = argument7;
    }

    public RequiredHeptArgumentBranch<S, T1, T2, T3, T4, T5, T6, T7, C, T> execute(@NotNull NonaConsumer<T1, T2, T3, T4, T5, T6, T7, S, C> action) {
        super.execute((s, ctx) -> action.accept(ctx.getArgument(argument1),
                                                ctx.getArgument(argument2),
                                                ctx.getArgument(argument3),
                                                ctx.getArgument(argument4),
                                                ctx.getArgument(argument5),
                                                ctx.getArgument(argument6),
                                                ctx.getArgument(argument7),
                                                s,
                                                ctx));
        return this;
    }

    public RequiredHeptArgumentBranch<S, T1, T2, T3, T4, T5, T6, T7, C, T> child(@NotNull HeptFunction<ArgRef<T1>, ArgRef<T2>, ArgRef<T3>, ArgRef<T4>, ArgRef<T5>, ArgRef<T6>, ArgRef<T7>, T> factory) {
        super.child(factory.apply(ArgRef.of(argument1), ArgRef.of(argument2), ArgRef.of(argument3), ArgRef.of(argument4), ArgRef.of(argument5), ArgRef.of(argument6), ArgRef.of(argument7)));
        return this;
    }
}
