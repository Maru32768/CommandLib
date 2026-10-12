package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.command.CommandExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;

public interface ArgumentBranchDelegate<C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    void description(@NotNull String description);

    void description(@NotNull Function<C, String> description);

    void execute(@NotNull CommandExecutor<C> action);

    void addChildren(@NotNull Collection<? extends T> children);

    void permission(@NotNull String node);

    void permission(@NotNull DefaultPermission defaultPermission);

    void permission(@NotNull DefaultPermission defaultPermission, @NotNull String description);

    void permission(@NotNull String node, @NotNull DefaultPermission defaultPermission);

    void permission(@NotNull String node, @NotNull DefaultPermission defaultPermission, @NotNull String description);

    void permissionDescription(@NotNull String description);
}
