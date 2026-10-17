package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.util.nms.command.NMSCommandListenerWrapper;
import dev.marulabs.commandlib.util.text.RichText;
import dev.marulabs.commandlib.util.text.RichTexts;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CommandContext extends CommonCommandContext<Object, BaseComponent> {
    public CommandContext(com.mojang.brigadier.context.CommandContext<Object> ctx) {
        super(ctx);
    }

    @Nullable
    public Entity getEntity() {
        return NMSCommandListenerWrapper.create(handle.getSource())
                                        .getBukkitEntity();
    }

    @Nullable
    public World getWorld() {
        return NMSCommandListenerWrapper.create(handle.getSource())
                                        .getBukkitWorld();
    }

    /**
     * The location the command runs at: the source's position in its world, with the source's rotation as the yaw and
     * pitch, such as the direction a player executing it faces. Null when the source has no world.
     */
    @Nullable
    public Location getLocation() {
        return NMSCommandListenerWrapper.create(handle.getSource())
                                        .getBukkitLocation();
    }

    public CommandSender getSender() {
        return NMSCommandListenerWrapper.create(handle.getSource())
                                        .getBukkitSender();
    }

    @Override
    @NotNull
    public CommandActor getActor() {
        return new BukkitCommandActor(getSender());
    }

    @NotNull
    public Player getPlayer() {
        CommandSender sender = getSender();
        if (!(sender instanceof Player)) {
            throw new IllegalStateException("sender is not a player");
        }
        return (Player) sender;
    }

    @NotNull
    public ConsoleCommandSender getConsole() {
        CommandSender sender = getSender();
        if (!(sender instanceof ConsoleCommandSender)) {
            throw new IllegalStateException("sender is not a console");
        }
        return (ConsoleCommandSender) sender;
    }

    @Override
    @NotNull
    public String getLanguage() {
        CommandSender sender = getSender();
        if (sender instanceof Player) {
            String language = ((Player) sender).getLocale();
            if (language != null && !language.isEmpty()) {
                return language.toLowerCase(Locale.ROOT);
            }
        }

        return super.getLanguage();
    }

    @Override
    public void sendMessage(@Nullable String message) {
        sendMessage(new TextComponent(String.valueOf(message)));
    }

    public void sendMessage(@NotNull BaseComponent component) {
        Objects.requireNonNull(component);
        deliver(component,
                () -> getSender().spigot()
                                 .sendMessage(component));
    }

    @Override
    public void sendSuccess(@Nullable String message, boolean broadcastToOps) {
        sendSuccess(new TextComponent(String.valueOf(message)), broadcastToOps);
    }

    /**
     * Sends {@code component} in green unless it has its own color, as command feedback like
     * {@link #sendSuccess(String, boolean)} without broadcasting it.
     */
    public void sendSuccess(@NotNull BaseComponent component) {
        sendSuccess(component, false);
    }

    /**
     * Sends {@code component} in green unless it has its own color, as command feedback like
     * {@link #sendSuccess(String, boolean)}.
     */
    public void sendSuccess(@NotNull BaseComponent component, boolean broadcastToOps) {
        BaseComponent colored = withDefaultColor(component, ChatColor.GREEN);
        deliver(colored,
                () -> NMSCommandListenerWrapper.create(handle.getSource())
                                               .sendSuccess(colored, broadcastToOps));
    }

    @Override
    public void sendSuccess(@NotNull RichText text, boolean broadcastToOps) {
        sendSuccess(RichTexts.toBungee(text), broadcastToOps);
    }

    @Override
    public void sendWarn(@Nullable String message) {
        sendWarn(new TextComponent(String.valueOf(message)));
    }

    public void sendWarn(@NotNull BaseComponent component) {
        sendMessage(withDefaultColor(component, ChatColor.YELLOW));
    }

    @Override
    public void sendFailure(@Nullable String message) {
        sendFailure(new TextComponent(String.valueOf(message)));
    }

    public void sendFailure(@NotNull BaseComponent component) {
        sendMessage(withDefaultColor(component, ChatColor.RED));
    }

    @Override
    public void sendMessage(@NotNull RichText text) {
        sendMessage(RichTexts.toBungee(text));
    }

    @Override
    Message toFailureMessage(List<BaseComponent> messages) {
        TextComponent joined = new TextComponent();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                joined.addExtra("\n");
            }
            joined.addExtra(messages.get(i));
        }
        return NMSCommandListenerWrapper.create(handle.getSource())
                                        .toMessage(joined);
    }

    private static BaseComponent withDefaultColor(BaseComponent component, ChatColor color) {
        if (component.getColorRaw() != null) {
            return component;
        }
        // Color a copy so that the caller's component is not changed.
        BaseComponent copy = component.duplicate();
        copy.setColor(color);
        return copy;
    }
}
