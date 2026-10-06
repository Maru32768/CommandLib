package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public final class CommandContext extends CommonCommandContext<Object, BaseComponent> {
    public CommandContext(com.mojang.brigadier.context.CommandContext<Object> ctx) {
        super(ctx);
    }

    public Entity getEntity() {
        return NMSCommandListenerWrapper.create(handle.getSource())
                                        .getBukkitEntity();
    }

    public World getWorld() {
        return NMSCommandListenerWrapper.create(handle.getSource())
                                        .getBukkitWorld();
    }

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
        getSender().spigot()
                   .sendMessage(Objects.requireNonNull(component));
    }

    @Override
    public void sendSuccess(@Nullable String message) {
        sendSuccess(new TextComponent(String.valueOf(message)));
    }

    public void sendSuccess(@NotNull BaseComponent component) {
        component.setColor(ChatColor.GREEN);
        sendMessage(component);
    }

    @Override
    public void sendWarn(@Nullable String message) {
        sendWarn(new TextComponent(String.valueOf(message)));
    }

    public void sendWarn(@NotNull BaseComponent component) {
        component.setColor(ChatColor.YELLOW);
        sendMessage(component);
    }

    @Override
    public void sendFailure(@Nullable String message) {
        sendFailure(new TextComponent(String.valueOf(message)));
    }

    public void sendFailure(@NotNull BaseComponent component) {
        component.setColor(ChatColor.RED);
        sendMessage(component);
    }

    @Override
    public void sendMessageWithOption(@Nullable String message, @NotNull Consumer<MessageOption> options) {
        TextComponent messageComponent = MessageOption.createMessage(options, (rgb, hoverText) -> {
            TextComponent component = new TextComponent(String.valueOf(message));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));
            component.setColor(ChatColor.of(new Color(rgb)));

            return component;
        });

        sendMessage(messageComponent);
    }

    @Override
    public void sendComponent(BaseComponent component) {
        sendMessage(component);
    }
}
