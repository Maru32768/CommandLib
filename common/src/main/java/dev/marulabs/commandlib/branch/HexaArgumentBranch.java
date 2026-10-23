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
import dev.marulabs.commandlib.command.Prerequisite;
import dev.marulabs.commandlib.internal.ArgumentBranchDelegate;
import dev.marulabs.commandlib.util.function.HeptConsumer;
import dev.marulabs.commandlib.util.function.HeptFunction;
import dev.marulabs.commandlib.util.function.HexaFunction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

public final class HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> extends ArgumentBranch<C, T> {
    private final CommonArgument<T1, C, ?> argument1;
    private final CommonArgument<T2, C, ?> argument2;
    private final CommonArgument<T3, C, ?> argument3;
    private final CommonArgument<T4, C, ?> argument4;
    private final CommonArgument<T5, C, ?> argument5;
    private final CommonArgument<T6, C, ?> argument6;

    @ApiStatus.Internal
    public HexaArgumentBranch(ArgumentBranchDelegate<C, T> delegate,
                              CommonArgument<T1, C, ?> argument1,
                              CommonArgument<T2, C, ?> argument2,
                              CommonArgument<T3, C, ?> argument3,
                              CommonArgument<T4, C, ?> argument4,
                              CommonArgument<T5, C, ?> argument5,
                              CommonArgument<T6, C, ?> argument6) {
        super(delegate);
        this.argument1 = argument1;
        this.argument2 = argument2;
        this.argument3 = argument3;
        this.argument4 = argument4;
        this.argument5 = argument5;
        this.argument6 = argument6;
    }

    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> execute(@NotNull HeptConsumer<T1, T2, T3, T4, T5, T6, C> action) {
        super.execute(ctx -> action.accept(ctx.getArgument(argument1),
                                           ctx.getArgument(argument2),
                                           ctx.getArgument(argument3),
                                           ctx.getArgument(argument4),
                                           ctx.getArgument(argument5),
                                           ctx.getArgument(argument6),
                                           ctx));
        return this;
    }

    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> executeAsync(@NotNull HeptFunction<T1, T2, T3, T4, T5, T6, C, CompletionStage<?>> action) {
        super.executeAsync(ctx -> action.apply(ctx.getArgument(argument1),
                                               ctx.getArgument(argument2),
                                               ctx.getArgument(argument3),
                                               ctx.getArgument(argument4),
                                               ctx.getArgument(argument5),
                                               ctx.getArgument(argument6),
                                               ctx));
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> execute(@NotNull CommandExecutor<C> action) {
        super.execute(action);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> executeAsync(@NotNull AsyncCommandExecutor<C> action) {
        super.executeAsync(action);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> description(@NotNull String description) {
        super.description(description);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> description(@NotNull Function<C, String> description) {
        super.description(description);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> description(@NotNull Caption caption) {
        super.description(caption);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> permission(@NotNull String node) {
        super.permission(node);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> permission(@NotNull DefaultPermission defaultPermission) {
        super.permission(defaultPermission);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> permission(@NotNull DefaultPermission defaultPermission,
                                                                       @NotNull String description) {
        super.permission(defaultPermission, description);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> permission(@NotNull String node,
                                                                       @NotNull DefaultPermission defaultPermission) {
        super.permission(node, defaultPermission);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> permission(@NotNull String node,
                                                                       @NotNull DefaultPermission defaultPermission,
                                                                       @NotNull String description) {
        super.permission(node, defaultPermission, description);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> permissionDescription(@NotNull String description) {
        super.permissionDescription(description);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> visibleWhen(@NotNull Predicate<? super C> predicate) {
        super.visibleWhen(predicate);
        return this;
    }

    @Override
    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> addPrerequisite(@NotNull Prerequisite<C> prerequisite) {
        super.addPrerequisite(prerequisite);
        return this;
    }

    public HexaArgumentBranch<T1, T2, T3, T4, T5, T6, C, T> child(@NotNull HexaFunction<ArgRef<T1>, ArgRef<T2>, ArgRef<T3>, ArgRef<T4>, ArgRef<T5>, ArgRef<T6>, T> factory) {
        child(factory.apply(ArgRef.of(argument1),
                            ArgRef.of(argument2),
                            ArgRef.of(argument3),
                            ArgRef.of(argument4),
                            ArgRef.of(argument5),
                            ArgRef.of(argument6)));
        return this;
    }

    @Override
    public <S> RequiredHexaArgumentBranch<S, T1, T2, T3, T4, T5, T6, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredHexaArgumentBranch<>(extractor,
                                                this::executeAsync,
                                                children -> delegate.addChildren(children),
                                                argument1,
                                                argument2,
                                                argument3,
                                                argument4,
                                                argument5,
                                                argument6);
    }
}
