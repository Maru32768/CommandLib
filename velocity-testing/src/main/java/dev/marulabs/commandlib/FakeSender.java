package dev.marulabs.commandlib;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * A Mockito {@link Player} or {@link ConsoleCommandSource} that records the messages sent to it. It has every
 * permission, as if a permission plugin granted them, unless restricted with {@link #permissions} or
 * {@link #denyPermissions}.
 */
public final class FakeSender {
    private final CommandSource source;
    private final List<Component> sentMessages = new ArrayList<>();

    public static FakeSender player(@NotNull String name) {
        return player(name, UUID.randomUUID());
    }

    public static FakeSender player(@NotNull String name, @NotNull UUID uniqueId) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUsername())
               .thenReturn(name);
        Mockito.when(player.getUniqueId())
               .thenReturn(uniqueId);
        return new FakeSender(player);
    }

    public static FakeSender console() {
        return new FakeSender(Mockito.mock(ConsoleCommandSource.class));
    }

    private FakeSender(CommandSource source) {
        this.source = source;
        Mockito.doAnswer(invocation -> {
            sentMessages.add(invocation.getArgument(0));
            return null;
        })
               .when(source)
               .sendMessage(Mockito.any(Component.class));
        answerPermissions(node -> Tristate.TRUE);
    }

    /**
     * Returns the underlying Mockito mock, useful for additional Mockito setup.
     */
    public CommandSource asSender() {
        return source;
    }

    /**
     * Returns the player's name, or {@code CONSOLE}.
     */
    public String getName() {
        return source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";
    }

    public Optional<UUID> getUniqueId() {
        return source instanceof Player ? Optional.of(((Player) source).getUniqueId()) : Optional.empty();
    }

    /**
     * Sets the player's locale, which {@link CommandContext#getLanguage()} reads.
     *
     * @throws IllegalStateException if the sender is the console
     */
    public FakeSender locale(@NotNull Locale locale) {
        if (!(source instanceof Player)) {
            throw new IllegalStateException("sender is not a player");
        }
        Mockito.when(((Player) source).getEffectiveLocale())
               .thenReturn(locale);
        return this;
    }

    /**
     * Grants only the given permission nodes and leaves every other one {@code UNDEFINED}, replacing any earlier call.
     * Like a real proxy, the tester then grants an unset node only if its default is {@link DefaultPermission#ALL}.
     * Without nodes, the sender is a player on a proxy without a permission plugin.
     */
    public FakeSender permissions(@NotNull String... permissions) {
        return permissions(permissions, new String[0]);
    }

    /**
     * Grants {@code granted}, denies {@code denied}, which wins, and leaves every other node {@code UNDEFINED}.
     */
    FakeSender permissions(String[] granted, String[] denied) {
        Set<String> grantedSet = Set.copyOf(Arrays.asList(granted));
        Set<String> deniedSet = Set.copyOf(Arrays.asList(denied));
        return answerPermissions(node -> deniedSet.contains(node) ? Tristate.FALSE
                : grantedSet.contains(node) ? Tristate.TRUE : Tristate.UNDEFINED);
    }

    /**
     * Denies the given permission nodes and grants every other one, replacing any earlier call.
     */
    public FakeSender denyPermissions(@NotNull String... permissions) {
        Set<String> denied = Set.copyOf(Arrays.asList(permissions));
        return answerPermissions(node -> Tristate.fromBoolean(!denied.contains(node)));
    }

    private FakeSender answerPermissions(Function<String, Tristate> permissions) {
        Mockito.when(source.getPermissionValue(Mockito.anyString()))
               .thenAnswer(invocation -> permissions.apply(invocation.getArgument(0)));
        Mockito.when(source.hasPermission(Mockito.anyString()))
               .thenAnswer(invocation -> permissions.apply(invocation.getArgument(0))
                                                    .asBoolean());
        return this;
    }

    /**
     * Returns all Adventure components sent to this sender.
     */
    public List<Component> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    /**
     * Returns the sent messages as plain text.
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
