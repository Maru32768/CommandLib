package dev.marulabs.cadence.internal;

import dev.marulabs.cadence.Caption;
import dev.marulabs.cadence.CommonCommand;
import dev.marulabs.cadence.CommonCommandContext;
import dev.marulabs.cadence.DefaultPermission;
import dev.marulabs.cadence.command.AsyncCommandExecutor;
import dev.marulabs.cadence.command.Prerequisite;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * What an argument branch changes on the command that created it. Public only because the branch classes are in
 * another package.
 */
@ApiStatus.Internal
public interface ArgumentBranchDelegate<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    void description(@NotNull String description);

    void description(@NotNull Function<C, String> description);

    void description(@NotNull Caption caption);

    void execute(@NotNull AsyncCommandExecutor<C> action);

    void addChildren(@NotNull Collection<? extends T> children);

    void permission(@NotNull String node);

    void permission(@NotNull DefaultPermission defaultPermission);

    void permission(@NotNull DefaultPermission defaultPermission, @NotNull String description);

    void permission(@NotNull String node, @NotNull DefaultPermission defaultPermission);

    void permission(@NotNull String node, @NotNull DefaultPermission defaultPermission, @NotNull String description);

    void permissionDescription(@NotNull String description);

    void visibleWhen(@NotNull Predicate<? super C> predicate);

    void addPrerequisite(@NotNull Prerequisite<C> prerequisite);
}
