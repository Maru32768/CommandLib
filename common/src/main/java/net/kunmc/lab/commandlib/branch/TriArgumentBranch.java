package net.kunmc.lab.commandlib.branch;

import net.kunmc.lab.commandlib.ArgRef;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.CommonArgument;
import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.DefaultPermission;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.command.Extractor;
import net.kunmc.lab.commandlib.util.function.TetraConsumer;
import net.kunmc.lab.commandlib.util.function.TriFunction;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TriArgumentBranch<T1, T2, T3, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends ArgumentBranch<C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;
    private final CommonArgument<T3, C, ?> argument3;

    public TriArgumentBranch(ArgumentBranchDelegate<C, T> delegate,
                             CommonArgument<T1, C, ?> argument1,
                             CommonArgument<T2, C, ?> argument2,
                             CommonArgument<T3, C, ?> argument3) {
        super(delegate);
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
    }

    public TriArgumentBranch<T1, T2, T3, C, T> execute(@NotNull TetraConsumer<T1, T2, T3, C> action) {
        super.execute(ctx -> action.accept(ctx.getArgument(argument1),
                                           ctx.getArgument(argument2),
                                           ctx.getArgument(argument3),
                                           ctx));
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> execute(@Nullable CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> description(@NotNull String description) {
        super.description(description);
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> description(@NotNull Function<C, String> description) {
        super.description(description);
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> permission(@NotNull String node) {
        super.permission(node);
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> permission(@NotNull DefaultPermission defaultPermission) {
        super.permission(defaultPermission);
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> permission(@NotNull DefaultPermission defaultPermission,
                                                          @NotNull String description) {
        super.permission(defaultPermission, description);
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> permission(@NotNull String node,
                                                          @NotNull DefaultPermission defaultPermission) {
        super.permission(node, defaultPermission);
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> permission(@NotNull String node,
                                                          @NotNull DefaultPermission defaultPermission,
                                                          @NotNull String description) {
        super.permission(node, defaultPermission, description);
        return this;
    }

    @Override
    public TriArgumentBranch<T1, T2, T3, C, T> permissionDescription(@NotNull String description) {
        super.permissionDescription(description);
        return this;
    }

    public TriArgumentBranch<T1, T2, T3, C, T> child(@NotNull TriFunction<ArgRef<T1>, ArgRef<T2>, ArgRef<T3>, T> factory) {
        child(factory.apply(ArgRef.of(argument1), ArgRef.of(argument2), ArgRef.of(argument3)));
        return this;
    }

    public <S> RequiredTriArgumentBranch<S, T1, T2, T3, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredTriArgumentBranch<>(extractor,
                                               this::execute,
                                               children -> delegate.addChildren(children),
                                               argument1,
                                               argument2,
                                               argument3);
    }
}
