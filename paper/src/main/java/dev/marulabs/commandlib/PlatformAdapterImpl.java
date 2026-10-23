package dev.marulabs.commandlib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.ContextState;
import dev.marulabs.commandlib.internal.PlatformAdapter;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;

@SuppressWarnings("UnstableApiUsage")
final class PlatformAdapterImpl implements PlatformAdapter<CommandSourceStack, Component, CommandContext> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                               ContextState state) {
        return new CommandContext(ctx, state);
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

    @Override
    public void logError(String message, Throwable error) {
        PluginLogger.error(message, error);
    }
}
