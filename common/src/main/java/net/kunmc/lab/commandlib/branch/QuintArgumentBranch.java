package net.kunmc.lab.commandlib.branch;

import net.kunmc.lab.commandlib.ArgRef;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.CommonArgument;
import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.DefaultPermission;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.command.Extractor;
import net.kunmc.lab.commandlib.util.function.HexaConsumer;
import net.kunmc.lab.commandlib.util.function.QuintFunction;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class QuintArgumentBranch<T1, T2, T3, T4, T5, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends ArgumentBranch<C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;
    private final CommonArgument<T3, C, ?> argument3;
    private final CommonArgument<T4, C, ?> argument4;
    private final CommonArgument<T5, C, ?> argument5;

    public QuintArgumentBranch(ArgumentBranchDelegate<C, T> delegate,
                               CommonArgument<T1, C, ?> argument1,
                               CommonArgument<T2, C, ?> argument2,
                               CommonArgument<T3, C, ?> argument3,
                               CommonArgument<T4, C, ?> argument4,
                               CommonArgument<T5, C, ?> argument5) {
        super(delegate);
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
        this.argument4 = argument4;
        this.argument5 = argument5;
    }

    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> execute(@NotNull HexaConsumer<T1, T2, T3, T4, T5, C> action) {
        super.execute(ctx -> action.accept(ctx.getArgument(argument1),
                                           ctx.getArgument(argument2),
                                           ctx.getArgument(argument3),
                                           ctx.getArgument(argument4),
                                           ctx.getArgument(argument5),
                                           ctx));
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> execute(@Nullable CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> description(@NotNull String description) {
        super.description(description);
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> description(@NotNull Function<C, String> description) {
        super.description(description);
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> permission(@NotNull String node) {
        super.permission(node);
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> permission(@NotNull DefaultPermission defaultPermission) {
        super.permission(defaultPermission);
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> permission(@NotNull DefaultPermission defaultPermission,
                                                                    @NotNull String description) {
        super.permission(defaultPermission, description);
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> permission(@NotNull String node,
                                                                    @NotNull DefaultPermission defaultPermission) {
        super.permission(node, defaultPermission);
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> permission(@NotNull String node,
                                                                    @NotNull DefaultPermission defaultPermission,
                                                                    @NotNull String description) {
        super.permission(node, defaultPermission, description);
        return this;
    }

    @Override
    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> permissionDescription(@NotNull String description) {
        super.permissionDescription(description);
        return this;
    }

    public QuintArgumentBranch<T1, T2, T3, T4, T5, C, T> child(@NotNull QuintFunction<ArgRef<T1>, ArgRef<T2>, ArgRef<T3>, ArgRef<T4>, ArgRef<T5>, T> factory) {
        child(factory.apply(ArgRef.of(argument1), ArgRef.of(argument2), ArgRef.of(argument3), ArgRef.of(argument4), ArgRef.of(argument5)));
        return this;
    }

    public <S> RequiredQuintArgumentBranch<S, T1, T2, T3, T4, T5, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredQuintArgumentBranch<>(extractor,
                                                 this::execute,
                                                 children -> delegate.addChildren(children),
                                                 argument1,
                                                 argument2,
                                                 argument3,
                                                 argument4,
                                                 argument5);
    }
}
