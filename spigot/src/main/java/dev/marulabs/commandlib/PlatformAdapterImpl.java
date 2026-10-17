package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.chat.NMSChatMessage;
import dev.marulabs.commandlib.util.nms.chat.NMSIChatMutableComponent;
import dev.marulabs.commandlib.util.nms.chat.NMSTranslatableContents;
import dev.marulabs.commandlib.util.nms.command.NMSCommandListenerWrapper;
import dev.marulabs.commandlib.util.nms.exception.NMSClassNotAssignableException;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlatformAdapterImpl implements PlatformAdapter<Object, BaseComponent, CommandContext> {
    private static final Logger LOGGER = Logger.getLogger(PlatformAdapterImpl.class.getName());

    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<Object> ctx) {
        return new CommandContext(ctx);
    }

    @Override
    public boolean hasPermission(Object commandSource, String permissionNode) {
        CommandSender sender;
        try {
            sender = NMSCommandListenerWrapper.create(commandSource)
                                              .getBukkitSender();
        } catch (UnsupportedOperationException | NullPointerException e) {
            // Data pack functions are compiled with a source that has no Bukkit sender: CraftBukkit's
            // CommandSource.NULL throws, and Mohist's reads the console of the source's server, which is null then. As
            // for vanilla commands, the function permission level guards them.
            return true;
        }
        if (isCraftBukkitNullSender(sender.getClass()
                                          .getName())) {
            return true;
        }
        return sender.hasPermission(permissionNode);
    }

    /**
     * From 1.21.11 CraftBukkit's CommandSource.NULL returns a NullCommandSender, which has no permission. Its package
     * is versioned up to 1.21.11, so only the CraftBukkit prefix and the class name are fixed.
     */
    static boolean isCraftBukkitNullSender(String className) {
        return className.startsWith("org.bukkit.craftbukkit.") && className.endsWith(".command.NullCommandSender");
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        try {
            return convertTranslatableCommandSyntaxException(e);
        } catch (RuntimeException conversionFailure) {
            // Exceptions thrown by custom argument types usually carry a plain Brigadier message (LiteralMessage) or a
            // literal component, which the NMS wrappers reject as not assignable. Anything else means the NMS bridge
            // itself is broken on this version, so report it while still sending a readable message.
            if (!hasCause(conversionFailure, NMSClassNotAssignableException.class)) {
                LOGGER.log(Level.WARNING,
                           "Failed to convert a CommandSyntaxException into a translatable message; sending its text instead.",
                           conversionFailure);
            }
            String message = e.getRawMessage()
                              .getString();
            return new ArgumentParseException(ctx -> ctx.sendFailure(message));
        }
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> causeClass) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (causeClass.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private ArgumentParseException convertTranslatableCommandSyntaxException(CommandSyntaxException e) {
        // The key and arguments are resolved here, not in the lambda, so that a message without a translation key
        // fails inside convertCommandSyntaxException and takes its fallback instead of failing when it is sent.
        TranslatableComponent component;
        if (NMSChatMessage.isSupportedVersion()) {
            NMSChatMessage msg = NMSChatMessage.create(e.getRawMessage());
            component = new TranslatableComponent(msg.getKey(), msg.getArgs());
        } else {
            NMSTranslatableContents contents = NMSIChatMutableComponent.create(e.getRawMessage())
                                                                       .getContentsAsTranslatable();
            component = new TranslatableComponent(contents.getKey(), contents.getArgs());
        }
        return new ArgumentParseException(ctx -> ((CommandContext) ctx).sendFailure(component));
    }
}
