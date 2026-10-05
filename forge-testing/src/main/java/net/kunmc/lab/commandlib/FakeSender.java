package net.kunmc.lab.commandlib;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A command sender for {@link CommandTester}: either a fake player backed by a {@link ServerPlayer} mock, or the
 * server console. Messages sent to the sender are recorded.
 *
 * <p>Like the other CommandLib testing artifacts, a sender has every permission node until
 * {@link #permissions(String...)} or {@link #denyPermissions(String...)} restricts it. {@link #op(boolean)} controls
 * the vanilla permission level used by vanilla checks such as entity selectors.</p>
 */
public final class FakeSender {
    private static final String CONSOLE_NAME = "Server";

    private final List<Component> sentMessages = new ArrayList<>();
    private final ServerPlayer player;
    private String name;
    private UUID uniqueId;
    private boolean op;
    private Predicate<String> permissionCheck = permission -> true;

    /**
     * Creates a non-operator fake player with the offline-mode UUID for the name.
     */
    public static FakeSender player(@NotNull String name) {
        return player(name,
                      UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
    }

    public static FakeSender player(@NotNull String name, @NotNull UUID uniqueId) {
        FakeSender sender = new FakeSender(Mockito.mock(ServerPlayer.class), name, uniqueId, false);
        sender.stubPlayer();
        return sender;
    }

    /**
     * Creates the server console, which is an operator.
     */
    public static FakeSender console() {
        return new FakeSender(null, CONSOLE_NAME, null, true);
    }

    private FakeSender(ServerPlayer player, String name, UUID uniqueId, boolean op) {
        this.player = player;
        this.name = name;
        this.uniqueId = uniqueId;
        this.op = op;
    }

    public FakeSender name(@NotNull String name) {
        this.name = name;
        if (player != null) {
            stubPlayer();
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public FakeSender uniqueId(@NotNull UUID uniqueId) {
        if (player == null) {
            throw new IllegalStateException("sender is not a player");
        }
        this.uniqueId = uniqueId;
        stubPlayer();
        return this;
    }

    public Optional<UUID> getUniqueId() {
        return Optional.ofNullable(uniqueId);
    }

    public boolean isPlayer() {
        return player != null;
    }

    /**
     * Returns the {@link ServerPlayer} mock of a fake player, useful for additional Mockito setup.
     */
    public Optional<ServerPlayer> asPlayer() {
        return Optional.ofNullable(player);
    }

    /**
     * Sets whether the sender has the operator permission level (4) instead of level 0.
     */
    public FakeSender op(boolean op) {
        this.op = op;
        if (player != null) {
            stubPlayer();
        }
        return this;
    }

    public boolean isOp() {
        return op;
    }

    /**
     * Grants only the given permission nodes.
     */
    public FakeSender permissions(@NotNull String... permissions) {
        Set<String> permissionSet = Set.of(permissions);
        permissionCheck = permissionSet::contains;
        return this;
    }

    /**
     * Grants every permission node except the given ones.
     */
    public FakeSender denyPermissions(@NotNull String... permissions) {
        Set<String> deniedPermissions = Set.of(permissions);
        permissionCheck = permission -> !deniedPermissions.contains(permission);
        return this;
    }

    public boolean hasPermission(@NotNull String permission) {
        return permissionCheck.test(permission);
    }

    /**
     * Returns all components sent to this sender during command execution.
     */
    public List<Component> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    /**
     * Convenience method that returns sent messages as plain text.
     */
    public List<String> getSentMessageTexts() {
        List<String> result = new ArrayList<>();
        for (Component component : sentMessages) {
            result.add(component.getString());
        }
        return result;
    }

    CommandSourceStack createSource(MinecraftServer server) {
        CommandSource source = player != null ? player : new ConsoleSource();
        return new CommandSourceStack(source,
                                      Vec3.ZERO,
                                      Vec2.ZERO,
                                      null,
                                      op ? 4 : 0,
                                      name,
                                      Component.nullToEmpty(name),
                                      server,
                                      player);
    }

    private void stubPlayer() {
        Component displayName = Component.nullToEmpty(name);
        Mockito.when(player.getName())
               .thenReturn(displayName);
        Mockito.when(player.getDisplayName())
               .thenReturn(displayName);
        Mockito.when(player.getScoreboardName())
               .thenReturn(name);
        Mockito.when(player.getGameProfile())
               .thenReturn(new GameProfile(uniqueId, name));
        Mockito.when(player.getUUID())
               .thenReturn(uniqueId);
        Mockito.when(player.getStringUUID())
               .thenReturn(uniqueId.toString());
        Mockito.when(player.isAlive())
               .thenReturn(true);
        Mockito.when(player.hasPermissions(Mockito.anyInt()))
               .thenAnswer(invocation -> op || (int) invocation.getArgument(0) <= 0);
        Mockito.when(player.acceptsSuccess())
               .thenReturn(true);
        Mockito.when(player.acceptsFailure())
               .thenReturn(true);
        Mockito.when(player.shouldInformAdmins())
               .thenReturn(false);
        //? if >=1.19 {
        Mockito.doAnswer(invocation -> {
                   sentMessages.add(invocation.getArgument(0));
                   return null;
               })
               .when(player)
               .sendSystemMessage(Mockito.any(Component.class));
        //?} else {
        /*Mockito.doAnswer(invocation -> {
                   sentMessages.add(invocation.getArgument(0));
                   return null;
               })
               .when(player)
               .sendMessage(Mockito.any(Component.class), Mockito.any(UUID.class));
        *///?}
    }

    private final class ConsoleSource implements CommandSource {
        //? if >=1.19 {
        @Override
        public void sendSystemMessage(Component component) {
            sentMessages.add(component);
        }
        //?} else {
        /*@Override
        public void sendMessage(Component component, UUID senderUuid) {
            sentMessages.add(component);
        }
        *///?}

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }
    }
}
