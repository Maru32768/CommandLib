package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

final class UnknownCommandActor implements CommandActor {
    static final UnknownCommandActor INSTANCE = new UnknownCommandActor();

    private UnknownCommandActor() {
    }

    @Override
    public @NotNull String getName() {
        return "unknown";
    }

    @Override
    public @NotNull Optional<UUID> getUniqueId() {
        return Optional.empty();
    }

    @Override
    public @NotNull CommandActorType getType() {
        return CommandActorType.UNKNOWN;
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public boolean isOperator() {
        return false;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return false;
    }

    @Override
    public @NotNull <T> Optional<T> unwrap(@NotNull Class<T> type) {
        return Optional.empty();
    }
}
