package net.kunmc.lab.commandlib;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
//? if >=1.18 {
import net.minecraft.server.level.ServerPlayer;
//?} else {
/*import net.minecraft.commands.CommandSource;
import net.minecraftforge.server.permission.PermissionAPI;

import java.lang.reflect.Field;
*///?}

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class ForgeCommandActor implements CommandActor {
    private final CommandSourceStack source;

    ForgeCommandActor(@NotNull CommandSourceStack source) {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public @NotNull String getName() {
        return source.getTextName();
    }

    @Override
    public @NotNull Optional<UUID> getUniqueId() {
        Entity entity = source.getEntity();
        return entity == null ? Optional.empty() : Optional.of(entity.getUUID());
    }

    @Override
    public @NotNull CommandActorType getType() {
        Entity entity = source.getEntity();
        if (entity instanceof Player) {
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
        return source.getEntity() instanceof Player;
    }

    @Override
    public boolean isOperator() {
        return source.hasPermission(4);
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        Entity entity = source.getEntity();
        //? if >=1.18 {
        if (entity instanceof ServerPlayer) {
            return CommandLib.hasPermission(source, (ServerPlayer) entity, Objects.requireNonNull(permission));
        }
        //?} else {
        /*if (entity instanceof Player) {
            return PermissionAPI.hasPermission((Player) entity, Objects.requireNonNull(permission));
        }
        *///?}
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
        //? if >=1.18 {
        return source.source;
        //?} else {
        /*if (RAW_SOURCE_FIELD == null) {
            return null;
        }
        try {
            return RAW_SOURCE_FIELD.get(source);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        *///?}
    }

    //? if <1.18 {
    /*// The field is private here and named differently in development (Mojang) and production (SRG),
    // so look it up by its type. CommandSourceStack has exactly one CommandSource field.
    private static final Field RAW_SOURCE_FIELD = findRawSourceField();

    private static Field findRawSourceField() {
        for (Field field : CommandSourceStack.class.getDeclaredFields()) {
            if (field.getType() == CommandSource.class) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
    *///?}
}
