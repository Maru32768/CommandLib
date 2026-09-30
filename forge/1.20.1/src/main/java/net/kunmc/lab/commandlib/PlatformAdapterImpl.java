package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.text.TextComponentBuilderImpl;
import net.kunmc.lab.commandlib.util.text.TranslatableComponentBuilderImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class PlatformAdapterImpl implements PlatformAdapter<CommandSourceStack, Component, CommandContext, Command> {
    @Override
    public CommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return new CommandContext(ctx);
    }

    @Override
    public boolean hasPermission(Command command, CommandSourceStack commandSource, String permissionPrefix) {
        return hasPermission(commandSource, command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(Command command, CommandContext ctx, String permissionPrefix) {
        return hasPermission(ctx.getSender(), command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(CommandSourceStack commandSource, String permissionNode) {
        if (commandSource.getEntity() instanceof ServerPlayer) {
            return CommandLib.hasPermission(commandSource, (ServerPlayer) commandSource.getEntity(), permissionNode);
        }
        return true;
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        return new ArgumentParseException(ctx -> {
            ((CommandContext) ctx).sendMessage((Component) e.getRawMessage());
        });
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
