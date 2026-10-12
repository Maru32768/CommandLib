package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.ArgRef;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class UnaryArgumentBranch<T1, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends ArgumentBranch<C, T> {
    private final CommonArgument<T1, C, ?> argument1;

    public UnaryArgumentBranch(ArgumentBranchDelegate<C, T> delegate, CommonArgument<T1, C, ?> argument1) {
        super(delegate);
        this.argument1 = argument1;
    }

    public UnaryArgumentBranch<T1, C, T> execute(@NotNull BiConsumer<T1, C> action) {
        super.execute(ctx -> action.accept(ctx.getArgument(argument1), ctx));
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> execute(@Nullable CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> description(@NotNull String description) {
        super.description(description);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> description(@NotNull Function<C, String> description) {
        super.description(description);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> permission(@NotNull String node) {
        super.permission(node);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> permission(@NotNull DefaultPermission defaultPermission) {
        super.permission(defaultPermission);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> permission(@NotNull DefaultPermission defaultPermission,
                                                    @NotNull String description) {
        super.permission(defaultPermission, description);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> permission(@NotNull String node,
                                                    @NotNull DefaultPermission defaultPermission) {
        super.permission(node, defaultPermission);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> permission(@NotNull String node,
                                                    @NotNull DefaultPermission defaultPermission,
                                                    @NotNull String description) {
        super.permission(node, defaultPermission, description);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> permissionDescription(@NotNull String description) {
        super.permissionDescription(description);
        return this;
    }

    public UnaryArgumentBranch<T1, C, T> child(@NotNull Function<ArgRef<T1>, T> factory) {
        child(factory.apply(ArgRef.of(argument1)));
        return this;
    }

    public <S> RequiredUnaryArgumentBranch<S, T1, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredUnaryArgumentBranch<>(extractor,
                                                 this::execute,
                                                 children -> delegate.addChildren(children),
                                                 argument1);
    }
}
