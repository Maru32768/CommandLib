package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.ArgRef;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.util.function.HeptFunction;
import dev.marulabs.commandlib.util.function.OctoConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends ArgumentBranch<C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;
    private final CommonArgument<T3, C, ?> argument3;
    private final CommonArgument<T4, C, ?> argument4;
    private final CommonArgument<T5, C, ?> argument5;
    private final CommonArgument<T6, C, ?> argument6;
    private final CommonArgument<T7, C, ?> argument7;

    public HeptArgumentBranch(ArgumentBranchDelegate<C, T> delegate,
                              CommonArgument<T1, C, ?> argument1,
                              CommonArgument<T2, C, ?> argument2,
                              CommonArgument<T3, C, ?> argument3,
                              CommonArgument<T4, C, ?> argument4,
                              CommonArgument<T5, C, ?> argument5,
                              CommonArgument<T6, C, ?> argument6,
                              CommonArgument<T7, C, ?> argument7) {
        super(delegate);
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
        this.argument4 = argument4;
        this.argument5 = argument5;
        this.argument6 = argument6;
        this.argument7 = argument7;
    }

    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> execute(@NotNull OctoConsumer<T1, T2, T3, T4, T5, T6, T7, C> action) {
        super.execute(ctx -> action.accept(ctx.getArgument(argument1),
                                           ctx.getArgument(argument2),
                                           ctx.getArgument(argument3),
                                           ctx.getArgument(argument4),
                                           ctx.getArgument(argument5),
                                           ctx.getArgument(argument6),
                                           ctx.getArgument(argument7),
                                           ctx));
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> execute(@NotNull CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> description(@NotNull String description) {
        super.description(description);
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> description(@NotNull Function<C, String> description) {
        super.description(description);
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> permission(@NotNull String node) {
        super.permission(node);
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> permission(@NotNull DefaultPermission defaultPermission) {
        super.permission(defaultPermission);
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> permission(@NotNull DefaultPermission defaultPermission,
                                                                           @NotNull String description) {
        super.permission(defaultPermission, description);
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> permission(@NotNull String node,
                                                                           @NotNull DefaultPermission defaultPermission) {
        super.permission(node, defaultPermission);
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> permission(@NotNull String node,
                                                                           @NotNull DefaultPermission defaultPermission,
                                                                           @NotNull String description) {
        super.permission(node, defaultPermission, description);
        return this;
    }

    @Override
    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> permissionDescription(@NotNull String description) {
        super.permissionDescription(description);
        return this;
    }

    public HeptArgumentBranch<T1, T2, T3, T4, T5, T6, T7, C, T> child(@NotNull HeptFunction<ArgRef<T1>, ArgRef<T2>, ArgRef<T3>, ArgRef<T4>, ArgRef<T5>, ArgRef<T6>, ArgRef<T7>, T> factory) {
        child(factory.apply(ArgRef.of(argument1),
                            ArgRef.of(argument2),
                            ArgRef.of(argument3),
                            ArgRef.of(argument4),
                            ArgRef.of(argument5),
                            ArgRef.of(argument6),
                            ArgRef.of(argument7)));
        return this;
    }

    @Override
    public <S> RequiredHeptArgumentBranch<S, T1, T2, T3, T4, T5, T6, T7, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredHeptArgumentBranch<>(extractor,
                                                this::execute,
                                                children -> delegate.addChildren(children),
                                                argument1,
                                                argument2,
                                                argument3,
                                                argument4,
                                                argument5,
                                                argument6,
                                                argument7);
    }
}
