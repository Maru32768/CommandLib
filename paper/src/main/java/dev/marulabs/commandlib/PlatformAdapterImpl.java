package dev.marulabs.commandlib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;

@SuppressWarnings("UnstableApiUsage")
public final class PlatformAdapterImpl implements PlatformAdapter<CommandSourceStack, Component, CommandContext, Command> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return new CommandContext(ctx);
    }

    @Override
    public boolean hasPermission(Command command, CommandSourceStack commandSource, String permissionPrefix) {
        return CommandPermissions.hasPermission(commandSource.getSender(), command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(Command command, CommandContext ctx, String permissionPrefix) {
        return CommandPermissions.hasPermission(ctx.getSender(), command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(CommandSourceStack commandSource, String permissionNode) {
        return CommandPermissions.hasPermission(commandSource.getSender(), permissionNode);
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        Message message = e.getRawMessage();
        // A vanilla message is a Minecraft component, which only the server's serializer converts. A plain Brigadier
        // message, as custom argument types throw, needs no server, so it also converts in tests.
        Component component = message instanceof LiteralMessage ? Component.text(message.getString())
                : MessageComponentSerializer.message()
                                            .deserialize(message);
        return new ArgumentParseException(ctx -> ((CommandContext) ctx).sendFailure(component));
    }
}
