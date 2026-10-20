package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.ArgRef;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.internal.ArgumentBranchDelegate;
import dev.marulabs.commandlib.util.function.QuintConsumer;
import dev.marulabs.commandlib.util.function.QuintFunction;
import dev.marulabs.commandlib.util.function.TetraFunction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TetraArgumentBranch<T1, T2, T3, T4, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends ArgumentBranch<C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;
    private final CommonArgument<T3, C, ?> argument3;
    private final CommonArgument<T4, C, ?> argument4;

    @ApiStatus.Internal
    public TetraArgumentBranch(ArgumentBranchDelegate<C, T> delegate,
                               CommonArgument<T1, C, ?> argument1,
                               CommonArgument<T2, C, ?> argument2,
                               CommonArgument<T3, C, ?> argument3,
                               CommonArgument<T4, C, ?> argument4) {
        super(delegate);
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
        this.argument4 = argument4;
    }

    public TetraArgumentBranch<T1, T2, T3, T4, C, T> execute(@NotNull QuintConsumer<T1, T2, T3, T4, C> action) {
        super.execute(ctx -> action.accept(ctx.getArgument(argument1),
                                           ctx.getArgument(argument2),
                                           ctx.getArgument(argument3),
                                           ctx.getArgument(argument4),
                                           ctx));
        return this;
    }

    public TetraArgumentBranch<T1, T2, T3, T4, C, T> executeAsync(@NotNull QuintFunction<T1, T2, T3, T4, C, CompletionStage<?>> action) {
        super.executeAsync(ctx -> action.apply(ctx.getArgument(argument1),
                                               ctx.getArgument(argument2),
                                               ctx.getArgument(argument3),
                                               ctx.getArgument(argument4),
                                               ctx));
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> execute(@NotNull CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> executeAsync(@NotNull AsyncCommandExecutor<C> action) {
        super.executeAsync(action);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> description(@NotNull String description) {
        super.description(description);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> description(@NotNull Function<C, String> description) {
        super.description(description);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> permission(@NotNull String node) {
        super.permission(node);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> permission(@NotNull DefaultPermission defaultPermission) {
        super.permission(defaultPermission);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> permission(@NotNull DefaultPermission defaultPermission,
                                                                @NotNull String description) {
        super.permission(defaultPermission, description);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> permission(@NotNull String node,
                                                                @NotNull DefaultPermission defaultPermission) {
        super.permission(node, defaultPermission);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> permission(@NotNull String node,
                                                                @NotNull DefaultPermission defaultPermission,
                                                                @NotNull String description) {
        super.permission(node, defaultPermission, description);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> permissionDescription(@NotNull String description) {
        super.permissionDescription(description);
        return this;
    }

    @Override
    public TetraArgumentBranch<T1, T2, T3, T4, C, T> visibleWhen(@NotNull Predicate<? super C> predicate) {
        super.visibleWhen(predicate);
        return this;
    }

    public TetraArgumentBranch<T1, T2, T3, T4, C, T> child(@NotNull TetraFunction<ArgRef<T1>, ArgRef<T2>, ArgRef<T3>, ArgRef<T4>, T> factory) {
        child(factory.apply(ArgRef.of(argument1), ArgRef.of(argument2), ArgRef.of(argument3), ArgRef.of(argument4)));
        return this;
    }

    @Override
    public <S> RequiredTetraArgumentBranch<S, T1, T2, T3, T4, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredTetraArgumentBranch<>(extractor,
                                                 this::executeAsync,
                                                 children -> delegate.addChildren(children),
                                                 argument1,
                                                 argument2,
                                                 argument3,
                                                 argument4);
    }
}
