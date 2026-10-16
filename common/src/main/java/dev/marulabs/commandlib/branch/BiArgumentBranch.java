package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.ArgRef;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.util.function.TriConsumer;
import dev.marulabs.commandlib.util.function.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

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

    public BiArgumentBranch<T1, T2, C, T> executeAsync(@NotNull TriFunction<T1, T2, C, CompletionStage<?>> action) {
        super.executeAsync(ctx -> action.apply(ctx.getArgument(argument1), ctx.getArgument(argument2), ctx));
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> execute(@NotNull CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public BiArgumentBranch<T1, T2, C, T> executeAsync(@NotNull AsyncCommandExecutor<C> action) {
        super.executeAsync(action);
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

    @Override
    public BiArgumentBranch<T1, T2, C, T> visibleWhen(@NotNull Predicate<? super C> predicate) {
        super.visibleWhen(predicate);
        return this;
    }

    public BiArgumentBranch<T1, T2, C, T> child(@NotNull BiFunction<ArgRef<T1>, ArgRef<T2>, T> factory) {
        child(factory.apply(ArgRef.of(argument1), ArgRef.of(argument2)));
        return this;
    }

    @Override
    public <S> RequiredBiArgumentBranch<S, T1, T2, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredBiArgumentBranch<>(extractor,
                                              this::executeAsync,
                                              children -> delegate.addChildren(children),
                                              argument1,
                                              argument2);
    }
}
