package net.kunmc.lab.commandlib.branch;

import net.kunmc.lab.commandlib.ArgRef;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.CommonArgument;
import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.command.Extractor;
import net.kunmc.lab.commandlib.util.function.QuintConsumer;
import net.kunmc.lab.commandlib.util.function.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

public final class RequiredTriArgumentBranch<S, T1, T2, T3, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends RequiredArgumentBranch<S, C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;
    private final CommonArgument<T3, C, ?> argument3;

    public RequiredTriArgumentBranch(@NotNull Extractor<C, S> extractor,
                                     @NotNull Consumer<CommandExecutor<C>> executeRegistrar,
                                     @NotNull Consumer<Collection<? extends T>> childrenAdder,
                                     @NotNull CommonArgument<T1, C, ?> argument1,
                                     @NotNull CommonArgument<T2, C, ?> argument2,
                                     @NotNull CommonArgument<T3, C, ?> argument3) {
        super(extractor, executeRegistrar, childrenAdder);
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
    }

    public RequiredTriArgumentBranch<S, T1, T2, T3, C, T> execute(@NotNull QuintConsumer<T1, T2, T3, S, C> action) {
        super.execute((s, ctx) -> action.accept(ctx.getArgument(argument1),
                                                ctx.getArgument(argument2),
                                                ctx.getArgument(argument3),
                                                s,
                                                ctx));
        return this;
    }

    public RequiredTriArgumentBranch<S, T1, T2, T3, C, T> child(@NotNull TriFunction<ArgRef<T1>, ArgRef<T2>, ArgRef<T3>, T> factory) {
        super.child(factory.apply(ArgRef.of(argument1), ArgRef.of(argument2), ArgRef.of(argument3)));
        return this;
    }
}
