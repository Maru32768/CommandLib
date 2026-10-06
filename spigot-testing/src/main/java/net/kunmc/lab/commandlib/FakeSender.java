package net.kunmc.lab.commandlib;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FakeSender {
    private final CommandSender commandSender;
    private final List<BaseComponent> sentMessages = new ArrayList<>();

    public static FakeSender player(String name) {
        Player player = Mockito.mock(Player.class);
        Player.Spigot spigot = Mockito.mock(Player.Spigot.class);
        Mockito.when(player.getName())
               .thenReturn(name);
        Mockito.when(player.getUniqueId())
               .thenReturn(UUID.randomUUID());
        Mockito.when(player.spigot())
               .thenReturn(spigot);

        FakeSender fakeSender = new FakeSender(player);
        fakeSender.op(false);
        Mockito.doAnswer(invocation -> {
                   fakeSender.sentMessages.add(invocation.getArgument(0));
                   return null;
               })
               .when(spigot)
               .sendMessage(Mockito.any(BaseComponent.class));

        return fakeSender;
    }

    public static FakeSender player(String name, UUID uniqueId) {
        return player(name).uniqueId(uniqueId);
    }

    public static FakeSender console() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        CommandSender.Spigot spigot = Mockito.mock(CommandSender.Spigot.class);
        Mockito.when(console.getName())
               .thenReturn("Console");
        Mockito.when(console.spigot())
               .thenReturn(spigot);

        FakeSender fakeSender = new FakeSender(console);
        fakeSender.op(true);
        Mockito.doAnswer(invocation -> {
                   fakeSender.sentMessages.add(invocation.getArgument(0));
                   return null;
               })
               .when(spigot)
               .sendMessage(Mockito.any(BaseComponent.class));

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

    public FakeSender name(String name) {
        Mockito.when(commandSender.getName())
               .thenReturn(name);
        return this;
    }

    public String getName() {
        return commandSender.getName();
    }

    public FakeSender uniqueId(UUID uniqueId) {
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

    public FakeSender locale(String locale) {
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

    public FakeSender permissions(String... permissions) {
        Set<String> permissionSet = Set.of(permissions);
        Mockito.when(commandSender.hasPermission(Mockito.anyString()))
               .thenAnswer(invocation -> permissionSet.contains(invocation.getArgument(0)));
        return this;
    }

    public FakeSender denyPermissions(String... permissions) {
        Set<String> deniedPermissions = Set.of(permissions);
        Mockito.when(commandSender.hasPermission(Mockito.anyString()))
               .thenAnswer(invocation -> !deniedPermissions.contains(invocation.getArgument(0)));
        return this;
    }

    CommandSender getCommandSender() {
        return commandSender;
    }

    /**
     * Returns all messages sent to this sender during command execution.
     * Use {@link BaseComponent#toPlainText()} to extract text,
     * or {@link BaseComponent#getColor()} to check the message color
     * (e.g. green for sendSuccess, red for sendFailure).
     */
    public List<BaseComponent> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    /**
     * Convenience method that returns sent messages as plain text with color codes stripped.
     */
    public List<String> getSentMessageTexts() {
        // Some messages embed legacy section-sign codes inside their text, which toPlainText keeps.
        return sentMessages.stream()
                           .map(c -> ChatColor.stripColor(c.toPlainText()))
                           .collect(Collectors.toList());
    }

    /**
     * Convenience method that returns sent messages as legacy text with color codes preserved.
     */
    public List<String> getSentMessageLegacyTexts() {
        return sentMessages.stream()
                           .map(c -> c.toLegacyText())
                           .collect(Collectors.toList());
    }
}
