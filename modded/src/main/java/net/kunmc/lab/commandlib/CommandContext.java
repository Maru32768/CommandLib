package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.util.Location;
import net.kunmc.lab.commandlib.util.text.RichText;
import net.kunmc.lab.commandlib.util.text.RichTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
//? if <1.19
/*import net.minecraft.network.chat.TextComponent;*/
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;

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
        sendMessage(literal(message), allowLogging);
    }

    public void sendMessage(@NotNull Component component) {
        sendMessage(component, false);
    }

    public void sendMessage(@NotNull Component component, boolean allowLogging) {
        //? if >=1.20 {
        getSender().sendSuccess(() -> Objects.requireNonNull(component), allowLogging);
        //?} else
        /*getSender().sendSuccess(Objects.requireNonNull(component), allowLogging);*/
    }

    @Override
    public void sendSuccess(@Nullable String message) {
        sendSuccess(message, false);
    }

    public void sendSuccess(@Nullable String message, boolean allowLogging) {
        sendSuccess(literal(message), allowLogging);
    }

    /**
     * Sends {@code component} in green unless it has its own color.
     */
    public void sendSuccess(@NotNull Component component) {
        sendSuccess(component, false);
    }

    public void sendSuccess(@NotNull Component component, boolean allowLogging) {
        sendMessage(withDefaultColor(component, ChatFormatting.GREEN), allowLogging);
    }

    @Override
    public void sendWarn(@Nullable String message) {
        sendWarn(message, false);
    }

    public void sendWarn(@Nullable String message, boolean allowLogging) {
        sendWarn(literal(message), allowLogging);
    }

    /**
     * Sends {@code component} in yellow unless it has its own color.
     */
    public void sendWarn(@NotNull Component component) {
        sendWarn(component, false);
    }

    public void sendWarn(@NotNull Component component, boolean allowLogging) {
        sendMessage(withDefaultColor(component, ChatFormatting.YELLOW), allowLogging);
    }

    @Override
    public void sendFailure(@Nullable String message) {
        sendFailure(message, false);
    }

    public void sendFailure(@Nullable String message, boolean allowLogging) {
        sendFailure(literal(message), allowLogging);
    }

    /**
     * Sends {@code component} in red unless it has its own color, through {@link CommandSourceStack#sendFailure}.
     */
    public void sendFailure(@NotNull Component component) {
        sendFailure(component, false);
    }

    public void sendFailure(@NotNull Component component, boolean allowLogging) {
        Component colored = withDefaultColor(component, ChatFormatting.RED);
        if (allowLogging) {
            // CommandSourceStack#sendFailure never broadcasts to ops, so route through sendSuccess.
            sendMessage(colored, true);
        } else {
            getSender().sendFailure(colored);
        }
    }

    @Override
    public void sendMessage(@NotNull RichText text) {
        sendMessage(RichTexts.toVanilla(text));
    }

    @Override
    public void sendFailure(@NotNull RichText text) {
        sendFailure(RichTexts.toVanilla(text));
    }

    @Override
    public void sendComponent(Component component) {
        sendMessage(component);
    }

    private static MutableComponent literal(@Nullable String message) {
        //? if >=1.19 {
        return Component.literal(String.valueOf(message));
        //?} else
        /*return new TextComponent(String.valueOf(message));*/
    }

    private static Component withDefaultColor(Component component, ChatFormatting color) {
        if (component.getStyle()
                     .getColor() != null) {
            return component;
        }
        // Color a copy so that the caller's component is not changed.
        MutableComponent copy = component.copy();
        return copy.setStyle(component.getStyle()
                                      .withColor(color));
    }
}
