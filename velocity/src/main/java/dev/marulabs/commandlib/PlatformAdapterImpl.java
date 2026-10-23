package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.ContextState;
import dev.marulabs.commandlib.internal.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.slf4j.LoggerFactory;

final class PlatformAdapterImpl implements PlatformAdapter<CommandSource, Component, CommandContext> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSource> ctx,
                                               ContextState state) {
        return new CommandContext(ctx, state);
    }

    @Override
    public boolean hasPermission(CommandSource commandSource, String permissionNode) {
        return VelocityPermissions.hasPermission(commandSource, permissionNode);
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        Message message = e.getRawMessage();
        // Velocity's messages (VelocityBrigadierMessage) carry a component.
        Component component = message instanceof ComponentLike ? ((ComponentLike) message).asComponent()
                : Component.text(message.getString());
        return new ArgumentParseException(ctx -> ((CommandContext) ctx).sendFailure(component));
    }

    @Override
    public void logError(String message, Throwable error) {
        LoggerFactory.getLogger("CommandLib")
                     .error(message, error);
    }
}
