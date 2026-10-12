package dev.marulabs.commandlib;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class BukkitCommandActor implements CommandActor {
    private final CommandSender sender;

    BukkitCommandActor(@NotNull CommandSender sender) {
        this.sender = Objects.requireNonNull(sender);
    }

    @Override
    public @NotNull String getName() {
        return sender.getName();
    }

    @Override
    public @NotNull Optional<UUID> getUniqueId() {
        if (sender instanceof Entity) {
            return Optional.of(((Entity) sender).getUniqueId());
        }
        return Optional.empty();
    }

    @Override
    public @NotNull CommandActorType getType() {
        if (sender instanceof RemoteConsoleCommandSender) {
            return CommandActorType.REMOTE_CONSOLE;
        }
        if (sender instanceof ConsoleCommandSender) {
            return CommandActorType.CONSOLE;
        }
        if (sender instanceof Player) {
            return CommandActorType.PLAYER;
        }
        if (sender instanceof Entity) {
            return CommandActorType.ENTITY;
        }
        if (sender instanceof BlockCommandSender) {
            return CommandActorType.COMMAND_BLOCK;
        }
        return CommandActorType.UNKNOWN;
    }

    @Override
    public boolean isConsole() {
        return getType() == CommandActorType.CONSOLE || getType() == CommandActorType.REMOTE_CONSOLE;
    }

    @Override
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    @Override
    public boolean isOperator() {
        return sender.isOp();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return sender.hasPermission(Objects.requireNonNull(permission));
    }

    @Override
    public @NotNull <T> Optional<T> unwrap(@NotNull Class<T> type) {
        Objects.requireNonNull(type);
        if (type.isInstance(sender)) {
            return Optional.of(type.cast(sender));
        }
        return Optional.empty();
    }
}
