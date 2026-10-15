package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
//? if >=1.18 {
import net.minecraft.server.level.ServerPlayer;
//?} else {
/*import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.permission.PermissionAPI;
*///?}

public final class PlatformAdapterImpl implements PlatformAdapter<CommandSourceStack, Component, CommandContext, Command> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return new CommandContext(ctx);
    }

    @Override
    public boolean hasPermission(Command command, CommandContext ctx, String permissionPrefix) {
        return hasPermission(ctx.getSender(), command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(CommandSourceStack commandSource, String permissionNode) {
        //? if >=1.18 {
        if (commandSource.getEntity() instanceof ServerPlayer) {
            return CommandLib.hasPermission(commandSource, (ServerPlayer) commandSource.getEntity(), permissionNode);
        }
        //?} else {
        /*if (commandSource.getEntity() instanceof Player) {
            return PermissionAPI.hasPermission((Player) commandSource.getEntity(), permissionNode);
        }
        *///?}
        return true;
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
