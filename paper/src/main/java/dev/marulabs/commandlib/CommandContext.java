package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.util.text.RichText;
import dev.marulabs.commandlib.util.text.RichTexts;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public final class CommandContext extends CommonCommandContext<CommandSourceStack, Component> {
    public CommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        super(ctx);
    }

    @NotNull
    public CommandSender getSender() {
        return handle.getSource()
                     .getSender();
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
    public String getLanguage() {
        CommandSender sender = getSender();
        if (sender instanceof Player) {
            String locale = ((Player) sender).getLocale();
            if (locale != null && !locale.isEmpty()) {
                return locale.toLowerCase(Locale.ROOT);
            }
        }
        return Locale.getDefault()
                     .toLanguageTag()
                     .toLowerCase(Locale.ROOT)
                     .replace('-', '_');
    }

    @Override
    public void sendMessage(@Nullable String message) {
        sendMessage(Component.text(String.valueOf(message)));
    }

    public void sendMessage(@NotNull Component component) {
        Objects.requireNonNull(component);
        deliver(component, () -> getSender().sendMessage(component));
    }

    @Override
    public void sendSuccess(@Nullable String message, boolean broadcastToOps) {
        sendSuccess(Component.text(String.valueOf(message)), broadcastToOps);
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
        Component colored = component.colorIfAbsent(NamedTextColor.GREEN);
        deliver(colored, () -> sendFeedback(colored, broadcastToOps));
    }

    @Override
    public void sendSuccess(@NotNull RichText text, boolean broadcastToOps) {
        sendSuccess(RichTexts.toAdventure(text), broadcastToOps);
    }

    private void sendFeedback(Component component, boolean broadcastToOps) {
        CommandSourceStack source = handle.getSource();
        Method sendSuccess;
        try {
            // The server's source is Minecraft's CommandSourceStack, which the Paper API does not expose.
            sendSuccess = source.getClass()
                                .getMethod("sendSuccess", Supplier.class, boolean.class);
        } catch (NoSuchMethodException e) {
            // Another source, such as a test mock, has no vanilla feedback.
            getSender().sendMessage(component);
            return;
        }
        // The serializer makes Minecraft's own component, which the Supplier passes as is.
        Message message = PaperMessages.toMessage(component);
        Supplier<Message> supplier = () -> message;
        try {
            sendSuccess.invoke(source, supplier, broadcastToOps);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not send command feedback", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Could not send command feedback", cause);
        }
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
    public void sendComponent(@NotNull Component component) {
        sendMessage(component);
    }

    @Override
    Message toFailureMessage(List<Component> messages) {
        return PaperMessages.toMessage(Component.join(JoinConfiguration.newlines(), messages));
    }
}
