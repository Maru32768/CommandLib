package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kyori.adventure.text.Component;

@SuppressWarnings("UnstableApiUsage")
public final class PlatformAdapterImpl implements PlatformAdapter<CommandSourceStack, Component, CommandContext, Command> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return new CommandContext(ctx);
    }

    @Override
    public boolean hasPermission(Command command, CommandSourceStack commandSource, String permissionPrefix) {
        return CommandLib.hasPermission(commandSource.getSender(), command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(Command command, CommandContext ctx, String permissionPrefix) {
        return CommandLib.hasPermission(ctx.getSender(), command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(CommandSourceStack commandSource, String permissionNode) {
        return CommandLib.hasPermission(commandSource.getSender(), permissionNode);
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        String message = e.getMessage();
        return new ArgumentParseException(ctx -> ctx.sendFailure(message != null ? message : e.getClass()
                                                                                              .getSimpleName()));
    }
}
