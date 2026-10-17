package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class PlatformAdapterImpl implements PlatformAdapter<CommandSourceStack, Component, CommandContext, Command> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return new CommandContext(ctx);
    }

    @Override
    public boolean hasPermission(CommandSourceStack commandSource, String permissionNode) {
        return new ForgeCommandActor(commandSource).hasPermission(permissionNode);
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        Message message = e.getRawMessage();
        return new ArgumentParseException(ctx -> {
            // Vanilla sends command errors as failures: red, and still shown where success feedback is silenced.
            if (message instanceof Component) {
                ((CommandContext) ctx).sendFailure((Component) message);
            } else {
                // Custom argument types may throw with a plain Brigadier message.
                ctx.sendFailure(message.getString());
            }
        });
    }
}
