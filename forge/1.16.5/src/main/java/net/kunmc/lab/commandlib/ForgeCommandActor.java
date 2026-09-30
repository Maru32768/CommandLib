package net.kunmc.lab.commandlib;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.server.permission.PermissionAPI;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class ForgeCommandActor implements CommandActor {
    private final CommandSource source;

    ForgeCommandActor(@NotNull CommandSource source) {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public @NotNull String getName() {
        return source.getName();
    }

    @Override
    public @NotNull Optional<UUID> getUniqueId() {
        Entity entity = source.getEntity();
        return entity == null ? Optional.empty() : Optional.of(entity.getUniqueID());
    }

    @Override
    public @NotNull CommandActorType getType() {
        Entity entity = source.getEntity();
        if (entity instanceof PlayerEntity) {
            return CommandActorType.PLAYER;
        }
        if (entity != null) {
            return CommandActorType.ENTITY;
        }

        Object rawSource = findRawSource();
        if (rawSource != null && rawSource.getClass()
                                          .getName()
                                          .contains("CommandBlock")) {
            return CommandActorType.COMMAND_BLOCK;
        }
        if (rawSource != null && rawSource.getClass()
                                          .getName()
                                          .contains("RCon")) {
            return CommandActorType.REMOTE_CONSOLE;
        }
        if (rawSource != null && rawSource.getClass()
                                          .getName()
                                          .contains("MinecraftServer")) {
            return CommandActorType.CONSOLE;
        }
        return CommandActorType.CONSOLE;
    }

    @Override
    public boolean isConsole() {
        return getType() == CommandActorType.CONSOLE || getType() == CommandActorType.REMOTE_CONSOLE;
    }

    @Override
    public boolean isPlayer() {
        return source.getEntity() instanceof PlayerEntity;
    }

    @Override
    public boolean isOperator() {
        return source.hasPermissionLevel(4);
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        Entity entity = source.getEntity();
        if (entity instanceof PlayerEntity) {
            return PermissionAPI.hasPermission((PlayerEntity) entity, Objects.requireNonNull(permission));
        }
        return false;
    }

    @Override
    public @NotNull <T> Optional<T> unwrap(@NotNull Class<T> type) {
        Objects.requireNonNull(type);
        if (type.isInstance(source)) {
            return Optional.of(type.cast(source));
        }

        Entity entity = source.getEntity();
        if (type.isInstance(entity)) {
            return Optional.of(type.cast(entity));
        }

        Object rawSource = findRawSource();
        if (type.isInstance(rawSource)) {
            return Optional.of(type.cast(rawSource));
        }

        return Optional.empty();
    }

    private Object findRawSource() {
        try {
            Field field = CommandSource.class.getDeclaredField("source");
            field.setAccessible(true);
            return field.get(source);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
