package net.kunmc.lab.commandlib.branch;

import net.kunmc.lab.commandlib.ArgRef;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.CommonArgument;
import net.kunmc.lab.commandlib.CommonCommand;
import net.kunmc.lab.commandlib.DefaultPermission;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.command.Extractor;
import net.kunmc.lab.commandlib.util.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class BiArgumentBranch<T1, T2, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends ArgumentBranch<C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;

    public BiArgumentBranch(ArgumentBranchDelegate<C, T> delegate,
                            CommonArgument<T1, C, ?> argument1,
                            CommonArgument<T2, C, ?> argument2) {
        super(delegate);
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    public BiArgumentBranch<T1, T2, C, T> execute(@NotNull TriConsumer<T1, T2, C> action) {
        super.execute(ctx -> action.accept(ctx.getArgument(argument1), ctx.getArgument(argument2), ctx));
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> execute(@Nullable CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> description(@NotNull String description) {
        super.description(description);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> description(@NotNull Function<C, String> description) {
        super.description(description);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> permission(@NotNull String node) {
        super.permission(node);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> permission(@NotNull DefaultPermission defaultPermission) {
        super.permission(defaultPermission);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> permission(@NotNull DefaultPermission defaultPermission,
                                                     @NotNull String description) {
        super.permission(defaultPermission, description);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> permission(@NotNull String node,
                                                     @NotNull DefaultPermission defaultPermission) {
        super.permission(node, defaultPermission);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> permission(@NotNull String node,
                                                     @NotNull DefaultPermission defaultPermission,
                                                     @NotNull String description) {
        super.permission(node, defaultPermission, description);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> permissionDescription(@NotNull String description) {
        super.permissionDescription(description);
        return this;
    }

    public BiArgumentBranch<T1, T2, C, T> child(@NotNull BiFunction<ArgRef<T1>, ArgRef<T2>, T> factory) {
        child(factory.apply(ArgRef.of(argument1), ArgRef.of(argument2)));
        return this;
    }

    public <S> RequiredBiArgumentBranch<S, T1, T2, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredBiArgumentBranch<>(extractor,
                                              this::execute,
                                              children -> delegate.addChildren(children),
                                              argument1,
                                              argument2);
    }
}
