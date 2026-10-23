package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.marulabs.commandlib.util.text.RichText;
import dev.marulabs.commandlib.util.text.RichTexts;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CommandContext extends CommonCommandContext<CommandSource, Component> {
    // Set by CommandLib.register, and by the tester from its first command until it closes. One proxy runs per JVM.
    static volatile @Nullable ProxyServer proxy;

    public CommandContext(com.mojang.brigadier.context.CommandContext<CommandSource> ctx) {
        super(ctx);
    }

    /**
     * The proxy that {@link CommandLib#register} was given.
     *
     * @throws IllegalStateException if no command has been registered
     */
    @NotNull
    public ProxyServer getProxy() {
        ProxyServer server = proxy;
        if (server == null) {
            throw new IllegalStateException("CommandLib.register has not been called");
        }
        return server;
    }

    @NotNull
    public CommandSource getSender() {
        return handle.getSource();
    }

    @Override
    @NotNull
    public CommandActor getActor() {
        return new VelocityCommandActor(getSender());
    }

    @NotNull
    public Player getPlayer() {
        CommandSource sender = getSender();
        if (!(sender instanceof Player)) {
            throw new IllegalStateException("sender is not a player");
        }
        return (Player) sender;
    }

    @NotNull
    public ConsoleCommandSource getConsole() {
        CommandSource sender = getSender();
        if (!(sender instanceof ConsoleCommandSource)) {
            throw new IllegalStateException("sender is not a console");
        }
        return (ConsoleCommandSource) sender;
    }

    @Override
    public String getLanguage() {
        CommandSource sender = getSender();
        if (sender instanceof Player) {
            Locale locale = ((Player) sender).getEffectiveLocale();
            if (locale != null) {
                return locale.toLanguageTag()
                             .toLowerCase(Locale.ROOT)
                             .replace('-', '_');
            }
        }
        return super.getLanguage();
    }

    @Override
    public void sendMessage(@Nullable String message) {
        sendMessage(Component.text(String.valueOf(message)));
    }

    public void sendMessage(@NotNull Component component) {
        Objects.requireNonNull(component);
        deliver(component, () -> getSender().sendMessage(component));
    }

    /**
     * Sends {@code message} in green. The proxy has no gamerules, so the sender always gets it.
     */
    @Override
    public void sendSuccess(@Nullable String message) {
        sendSuccess(Component.text(String.valueOf(message)));
    }

    /**
     * Sends {@code component} in green unless it has its own color.
     */
    public void sendSuccess(@NotNull Component component) {
        sendMessage(component.colorIfAbsent(NamedTextColor.GREEN));
    }

    /**
     * Sends {@code text} in green unless it has its own color.
     */
    @Override
    public void sendSuccess(@NotNull RichText text) {
        sendSuccess(RichTexts.toAdventure(text));
    }

    @Override
    public void sendWarn(@Nullable String message) {
        sendWarn(Component.text(String.valueOf(message)));
    }

    /**
     * Sends {@code component} in yellow unless it has its own color.
     */
    public void sendWarn(@NotNull Component component) {
        sendMessage(component.colorIfAbsent(NamedTextColor.YELLOW));
    }

    @Override
    public void sendFailure(@Nullable String message) {
        sendFailure(Component.text(String.valueOf(message)));
    }

    /**
     * Sends {@code component} in red unless it has its own color.
     */
    public void sendFailure(@NotNull Component component) {
        sendMessage(component.colorIfAbsent(NamedTextColor.RED));
    }

    @Override
    public void sendMessage(@NotNull RichText text) {
        sendMessage(RichTexts.toAdventure(text));
    }

    @Override
    Message toFailureMessage(List<Component> messages) {
        // Velocity sends the component of a command's syntax error in red unless it has its own color.
        return VelocityBrigadierMessage.tooltip(Component.join(JoinConfiguration.newlines(), messages));
    }
}
