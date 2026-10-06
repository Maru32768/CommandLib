package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.nms.chat.NMSChatMessage;
import net.kunmc.lab.commandlib.util.nms.chat.NMSIChatMutableComponent;
import net.kunmc.lab.commandlib.util.nms.chat.NMSTranslatableContents;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.exception.NMSClassNotAssignableException;
import net.kunmc.lab.commandlib.util.text.TextComponentBuilderImpl;
import net.kunmc.lab.commandlib.util.text.TranslatableComponentBuilderImpl;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlatformAdapterImpl implements PlatformAdapter<Object, BaseComponent, CommandContext, Command> {
    private static final Logger LOGGER = Logger.getLogger(PlatformAdapterImpl.class.getName());

    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<Object> ctx) {
        return new CommandContext(ctx);
    }

    @Override
    public boolean hasPermission(Command command, Object commandSource, String permissionPrefix) {
        return hasPermission(commandSource, command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(Command command, CommandContext ctx, String permissionPrefix) {
        return ctx.getSender()
                  .hasPermission(command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(Object commandSource, String permissionNode) {
        return NMSCommandListenerWrapper.create(commandSource)
                                        .getBukkitSender()
                                        .hasPermission(permissionNode);
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

    @Override
    public TextComponentBuilderImpl createTextComponentBuilder(@NotNull String text) {
        return new TextComponentBuilderImpl(text);
    }

    @Override
    public TranslatableComponentBuilderImpl createTranslatableComponentBuilder(@NotNull String key) {
        return new TranslatableComponentBuilderImpl(key);
    }
}
