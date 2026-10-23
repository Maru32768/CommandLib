package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.ArgRef;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.internal.ArgumentBranchDelegate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class UnaryArgumentBranch<T1, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends ArgumentBranch<C, T> {
    private final CommonArgument<T1, C, ?> argument1;

    @ApiStatus.Internal
    public UnaryArgumentBranch(ArgumentBranchDelegate<C, T> delegate, CommonArgument<T1, C, ?> argument1) {
        super(delegate);
        this.argument1 = argument1;
    }

    public UnaryArgumentBranch<T1, C, T> execute(@NotNull BiConsumer<T1, C> action) {
        super.execute(ctx -> action.accept(ctx.getArgument(argument1), ctx));
        return this;
    }

    public UnaryArgumentBranch<T1, C, T> executeAsync(@NotNull BiFunction<T1, C, CompletionStage<?>> action) {
        super.executeAsync(ctx -> action.apply(ctx.getArgument(argument1), ctx));
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> execute(@NotNull CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public UnaryArgumentBranch<T1, C, T> executeAsync(@NotNull AsyncCommandExecutor<C> action) {
        super.executeAsync(action);
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
    public UnaryArgumentBranch<T1, C, T> description(@NotNull Caption caption) {
        super.description(caption);
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

    @Override
    public UnaryArgumentBranch<T1, C, T> visibleWhen(@NotNull Predicate<? super C> predicate) {
        super.visibleWhen(predicate);
        return this;
    }

    public UnaryArgumentBranch<T1, C, T> child(@NotNull Function<ArgRef<T1>, T> factory) {
        child(factory.apply(ArgRef.of(argument1)));
        return this;
    }

    @Override
    public <S> RequiredUnaryArgumentBranch<S, T1, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredUnaryArgumentBranch<>(extractor,
                                                 this::executeAsync,
                                                 children -> delegate.addChildren(children),
                                                 argument1);
    }
}
