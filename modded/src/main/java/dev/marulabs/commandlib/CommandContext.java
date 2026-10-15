package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.util.Location;
import dev.marulabs.commandlib.util.text.RichText;
import dev.marulabs.commandlib.util.text.RichTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
//? if <1.19
/*import net.minecraft.network.chat.TextComponent;*/
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
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

    /**
     * The location the command runs at, with the source's rotation as the yaw and pitch, such as the direction a player
     * executing it faces or what {@code /execute rotated} sets.
     */
    public Location getLocation() {
        Vec3 position = handle.getSource()
                              .getPosition();
        Vec2 rotation = handle.getSource()
                              .getRotation();
        return new Location(getWorld(), position.x, position.y, position.z, rotation.y, rotation.x);
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
            Object value;
            try {
                Method method = source.getClass()
                                      .getMethod(methodName);
                value = method.invoke(source);
            } catch (ReflectiveOperationException e) {
                // The source has no such accessor, so try the next one.
                value = null;
            }
            if (value != null) {
                return value.toString();
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

    /**
     * Sends {@code component} through {@link CommandSourceStack#sendSuccess}, which respects the
     * {@code sendCommandFeedback} gamerule and, with {@code allowLogging}, broadcasts it to operators.
     */
    public void sendMessage(@NotNull Component component, boolean allowLogging) {
        Objects.requireNonNull(component);
        //? if >=1.20 {
        deliver(component, () -> getSender().sendSuccess(() -> component, allowLogging));
        //?} else
        /*deliver(component, () -> getSender().sendSuccess(component, allowLogging));*/
    }

    @Override
    public void sendSuccess(@Nullable String message, boolean broadcastToOps) {
        sendSuccess(literal(message), broadcastToOps);
    }

    /**
     * Sends {@code component} in green unless it has its own color, as command feedback like
     * {@link #sendSuccess(String, boolean)} without broadcasting it.
     */
    public void sendSuccess(@NotNull Component component) {
        sendSuccess(component, false);
    }

    /**
     * Sends {@code component} in green unless it has its own color, as command feedback like
     * {@link #sendSuccess(String, boolean)}.
     */
    public void sendSuccess(@NotNull Component component, boolean broadcastToOps) {
        sendMessage(withDefaultColor(component, ChatFormatting.GREEN), broadcastToOps);
    }

    @Override
    public void sendSuccess(@NotNull RichText text, boolean broadcastToOps) {
        sendSuccess(RichTexts.toVanilla(text), broadcastToOps);
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
            deliver(colored, () -> getSender().sendFailure(colored));
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

    @Override
    Message toFailureMessage(List<Component> messages) {
        MutableComponent joined = literal("");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                joined.append("\n");
            }
            joined.append(messages.get(i));
        }
        return joined;
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
