package dev.marulabs.commandlib;

import dev.marulabs.commandlib.internal.ContextState;
import dev.marulabs.commandlib.internal.PlatformAdapter;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;

final class PlatformAdapterImpl implements PlatformAdapter<CommandSourceStack, Component, CommandContext> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                               ContextState state) {
        return new CommandContext(ctx, state);
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

    @Override
    public void logError(String message, Throwable error) {
        LogManager.getLogger("CommandLib")
                  .error(message, error);
    }
}
