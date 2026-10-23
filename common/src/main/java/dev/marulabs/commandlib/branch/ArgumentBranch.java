package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.command.Prerequisite;
import dev.marulabs.commandlib.internal.ArgumentBranchDelegate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class ArgumentBranch<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    protected final ArgumentBranchDelegate<C, T> delegate;

    @ApiStatus.Internal
    public ArgumentBranch(@NotNull ArgumentBranchDelegate<C, T> delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    public ArgumentBranch<C, T> description(@NotNull String description) {
        delegate.description(Objects.requireNonNull(description));
        return this;
    }

    public ArgumentBranch<C, T> description(@NotNull Function<C, String> description) {
        delegate.description(Objects.requireNonNull(description));
        return this;
    }

    /**
     * Same as {@link CommonCommand#description(Caption)} for this branch.
     */
    public ArgumentBranch<C, T> description(@NotNull Caption caption) {
        delegate.description(Objects.requireNonNull(caption));
        return this;
    }

    public ArgumentBranch<C, T> execute(@NotNull CommandExecutor<C> action) {
        return executeAsync(AsyncCommandExecutor.of(action));
    }

    /**
     * Sets an executor whose work finishes later, like {@link CommonCommand#executeAsync(AsyncCommandExecutor)}.
     *
     * @throws IllegalStateException if the branch already has an executor
     */
    public ArgumentBranch<C, T> executeAsync(@NotNull AsyncCommandExecutor<C> action) {
        delegate.execute(Objects.requireNonNull(action));
        return this;
    }

    public ArgumentBranch<C, T> permission(@NotNull String node) {
        delegate.permission(Objects.requireNonNull(node));
        return this;
    }

    public ArgumentBranch<C, T> permission(@NotNull DefaultPermission defaultPermission) {
        delegate.permission(Objects.requireNonNull(defaultPermission));
        return this;
    }

    public ArgumentBranch<C, T> permission(@NotNull DefaultPermission defaultPermission, @NotNull String description) {
        delegate.permission(Objects.requireNonNull(defaultPermission), Objects.requireNonNull(description));
        return this;
    }

    public ArgumentBranch<C, T> permission(@NotNull String node, @NotNull DefaultPermission defaultPermission) {
        delegate.permission(Objects.requireNonNull(node), Objects.requireNonNull(defaultPermission));
        return this;
    }

    public ArgumentBranch<C, T> permission(@NotNull String node,
                                           @NotNull DefaultPermission defaultPermission,
                                           @NotNull String description) {
        delegate.permission(Objects.requireNonNull(node),
                            Objects.requireNonNull(defaultPermission),
                            Objects.requireNonNull(description));
        return this;
    }

    public ArgumentBranch<C, T> permissionDescription(@NotNull String description) {
        delegate.permissionDescription(Objects.requireNonNull(description));
        return this;
    }

    /**
     * Shows the branch only to senders for whom {@code predicate} holds, like
     * {@link CommonCommand#visibleWhen(Predicate)}.
     */
    public ArgumentBranch<C, T> visibleWhen(@NotNull Predicate<? super C> predicate) {
        delegate.visibleWhen(Objects.requireNonNull(predicate));
        return this;
    }

    /**
     * Adds a check that runs before this branch executes, after those of the command, like
     * {@link CommonCommand#addPrerequisite(Prerequisite)}. It applies to the child commands of the branch too, unless
     * they call {@link CommonCommand#disableParentPrerequisite()}. An {@link Extractor} checks what it would read, so
     * {@code addPrerequisite(Command.player())} lets only players run the branch.
     */
    public ArgumentBranch<C, T> addPrerequisite(@NotNull Prerequisite<C> prerequisite) {
        delegate.addPrerequisite(Objects.requireNonNull(prerequisite));
        return this;
    }

    @SafeVarargs
    public final ArgumentBranch<C, T> child(@NotNull T child, @NotNull T... children) {
        List<T> list = new ArrayList<>();
        list.add(child);
        Collections.addAll(list, children);
        delegate.addChildren(list);
        return this;
    }

    public <S> RequiredArgumentBranch<S, C, T> require(@NotNull Extractor<C, S> extractor) {
        return new RequiredArgumentBranch<>(Objects.requireNonNull(extractor),
                                            this::executeAsync,
                                            children -> delegate.addChildren(children));
    }
}
