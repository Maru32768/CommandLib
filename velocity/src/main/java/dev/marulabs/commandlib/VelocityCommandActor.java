package dev.marulabs.commandlib;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class VelocityCommandActor implements CommandActor {
    private final CommandSource source;

    VelocityCommandActor(@NotNull CommandSource source) {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public @NotNull String getName() {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        }
        return source instanceof ConsoleCommandSource ? "CONSOLE" : "unknown";
    }

    @Override
    public @NotNull Optional<UUID> getUniqueId() {
        if (source instanceof Player) {
            return Optional.of(((Player) source).getUniqueId());
        }
        return Optional.empty();
    }

    @Override
    public @NotNull CommandActorType getType() {
        if (source instanceof Player) {
            return CommandActorType.PLAYER;
        }
        return source instanceof ConsoleCommandSource ? CommandActorType.CONSOLE : CommandActorType.UNKNOWN;
    }

    @Override
    public boolean isConsole() {
        return source instanceof ConsoleCommandSource;
    }

    @Override
    public boolean isPlayer() {
        return source instanceof Player;
    }

    /**
     * The proxy has no operators, so only the console counts as one.
     */
    @Override
    public boolean isOperator() {
        return isConsole();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return VelocityPermissions.hasPermission(source, Objects.requireNonNull(permission));
    }

    @Override
    public @NotNull <T> Optional<T> unwrap(@NotNull Class<T> type) {
        Objects.requireNonNull(type);
        if (type.isInstance(source)) {
            return Optional.of(type.cast(source));
        }
        return Optional.empty();
    }
}
