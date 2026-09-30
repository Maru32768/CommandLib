package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.util.Location;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public final class CommandContext extends CommonCommandContext<CommandSourceStack, Component> {
    public CommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        super(ctx);
    }

    public Entity getEntity() {
        return handle.getSource()
                     .getEntity();
    }

    public ServerLevel getWorld() {
        return handle.getSource()
                     .getLevel();
    }

    public Location getLocation() {
        return new Location(getWorld(),
                            handle.getSource()
                                  .getPosition());
    }

    public @NotNull CommandSourceStack getSender() {
        return handle.getSource();
    }

    @Override
    @NotNull
    public String getLanguage() {
        String language = findLanguage(getSender());
        if (language == null) {
            language = findLanguage(getEntity());
        }
        if (language == null || language.isEmpty()) {
            return super.getLanguage();
        }

        return language.toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static String findLanguage(@Nullable Object source) {
        if (source == null) {
            return null;
        }

        for (String methodName : new String[]{"getLanguage", "getLocale"}) {
            try {
                Method method = source.getClass()
                                      .getMethod(methodName);
                Object value = method.invoke(source);
                if (value != null) {
                    return value.toString();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            Field field = source.getClass()
                                .getDeclaredField("language");
            field.setAccessible(true);
            Object value = field.get(source);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Override
    @NotNull
    public CommandActor getActor() {
        return new ForgeCommandActor(getSender());
    }

    @Override
    public void sendMessage(@Nullable String message) {
        sendMessage(message, false);
    }

    public void sendMessage(@Nullable String message, boolean allowLogging) {
        sendMessage(Component.literal(String.valueOf(message)), allowLogging);

    }

    public void sendMessage(@NotNull Component component) {
        sendMessage(component, false);
    }

    public void sendMessage(@NotNull Component component, boolean allowLogging) {
        getSender().sendSuccess(() -> Objects.requireNonNull(component), allowLogging);
    }

    @Override
    public void sendSuccess(@Nullable String message) {
        sendSuccess(message, false);
    }

    public void sendSuccess(@Nullable String message, boolean allowLogging) {
        MutableComponent component = Component.literal(String.valueOf(message))
                                             .withStyle(ChatFormatting.GREEN);
        sendMessage(component, allowLogging);
    }

    @Override
    public void sendWarn(@Nullable String message) {
        sendWarn(message, false);
    }

    public void sendWarn(@Nullable String message, boolean allowLogging) {
        MutableComponent component = Component.literal(String.valueOf(message))
                                             .withStyle(ChatFormatting.YELLOW);
        sendMessage(component, allowLogging);
    }

    @Override
    public void sendFailure(@Nullable String message) {
        sendFailure(message, false);
    }

    public void sendFailure(@Nullable String message, boolean allowLogging) {
        MutableComponent component = Component.literal(String.valueOf(message))
                                             .withStyle(ChatFormatting.RED);
        getSender().sendFailure(component);
    }

    @Override
    public void sendMessageWithOption(@Nullable String message, @NotNull Consumer<MessageOption> options) {
        Component messageComponent = MessageOption.createMessage(options, (rgb, hoverText) -> {
            MutableComponent component = Component.literal(String.valueOf(message));
            Style style = component.getStyle()
                                   .withColor(TextColor.fromRgb(rgb))
                                   .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                  Component.literal(hoverText)));
            return component.setStyle(style);
        });

        sendMessage(messageComponent);
    }

    @Override
    public void sendComponent(Component component) {
        sendMessage(component);
    }
}
