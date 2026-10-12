package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface CommandActor {
    @NotNull String getName();

    @NotNull Optional<UUID> getUniqueId();

    @NotNull CommandActorType getType();

    boolean isConsole();

    boolean isPlayer();

    boolean isOperator();

    boolean hasPermission(@NotNull String permission);

    @NotNull <T> Optional<T> unwrap(@NotNull Class<T> type);
}
