package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;

public final class PlatformAdapterImpl implements PlatformAdapter<CommandSource, Component, CommandContext> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSource> ctx) {
        return new CommandContext(ctx);
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
}
