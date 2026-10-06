package net.kunmc.lab.commandlib;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FakeSender {
    private final CommandSender commandSender;
    private final List<Component> sentMessages = new ArrayList<>();

    public static FakeSender player(@NotNull String name) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName())
               .thenReturn(name);
        Mockito.when(player.getUniqueId())
               .thenReturn(UUID.randomUUID());

        FakeSender fakeSender = new FakeSender(player);
        fakeSender.op(false);
        Mockito.doAnswer(invocation -> {
                   fakeSender.sentMessages.add(invocation.getArgument(0));
                   return null;
               })
               .when(player)
               .sendMessage(Mockito.any(Component.class));

        return fakeSender;
    }

    public static FakeSender player(@NotNull String name, @NotNull UUID uniqueId) {
        return player(name).uniqueId(uniqueId);
    }

    public static FakeSender console() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        Mockito.when(console.getName())
               .thenReturn("Console");

        FakeSender fakeSender = new FakeSender(console);
        fakeSender.op(true);
        Mockito.doAnswer(invocation -> {
                   fakeSender.sentMessages.add(invocation.getArgument(0));
                   return null;
               })
               .when(console)
               .sendMessage(Mockito.any(Component.class));

        return fakeSender;
    }

    private FakeSender(CommandSender commandSender) {
        this.commandSender = commandSender;
        Mockito.when(commandSender.hasPermission(Mockito.anyString()))
               .thenReturn(true);
    }

    /**
     * Returns the underlying CommandSender mock, useful for additional Mockito setup.
     */
    public CommandSender asSender() {
        return commandSender;
    }

    public FakeSender name(@NotNull String name) {
        Mockito.when(commandSender.getName())
               .thenReturn(name);
        return this;
    }

    public String getName() {
        return commandSender.getName();
    }

    public FakeSender uniqueId(@NotNull UUID uniqueId) {
        if (!(commandSender instanceof Entity)) {
            throw new IllegalStateException("sender is not an entity");
        }
        Mockito.when(((Entity) commandSender).getUniqueId())
               .thenReturn(uniqueId);
        return this;
    }

    public Optional<UUID> getUniqueId() {
        if (!(commandSender instanceof Entity)) {
            return Optional.empty();
        }
        return Optional.ofNullable(((Entity) commandSender).getUniqueId());
    }

    public FakeSender locale(@NotNull String locale) {
        if (!(commandSender instanceof Player)) {
            throw new IllegalStateException("sender is not a player");
        }
        Mockito.when(((Player) commandSender).getLocale())
               .thenReturn(locale);
        return this;
    }

    public String getLocale() {
        if (!(commandSender instanceof Player)) {
            return null;
        }
        return ((Player) commandSender).getLocale();
    }

    public FakeSender op(boolean op) {
        Mockito.when(commandSender.isOp())
               .thenReturn(op);
        return this;
    }

    public FakeSender permissions(@NotNull String... permissions) {
        Set<String> permissionSet = Set.of(permissions);
        Mockito.when(commandSender.hasPermission(Mockito.anyString()))
               .thenAnswer(invocation -> permissionSet.contains(invocation.getArgument(0)));
        return this;
    }

    public FakeSender denyPermissions(@NotNull String... permissions) {
        Set<String> deniedPermissions = Set.of(permissions);
        Mockito.when(commandSender.hasPermission(Mockito.anyString()))
               .thenAnswer(invocation -> !deniedPermissions.contains(invocation.getArgument(0)));
        return this;
    }

    /**
     * Returns all Adventure components sent to this sender during command execution.
     */
    public List<Component> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    /**
     * Convenience method that returns sent messages as plain text.
     */
    public List<String> getSentMessageTexts() {
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
        List<String> result = new ArrayList<>();
        for (Component component : sentMessages) {
            // Some messages embed legacy section-sign codes inside their text, which the plain serializer keeps.
            result.add(serializer.serialize(component)
                                 .replaceAll("(?i)§[0-9A-FK-ORX]", ""));
        }
        return result;
    }
}
