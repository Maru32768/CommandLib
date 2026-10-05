package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class PlayerArgument extends Argument<ServerPlayer, PlayerArgument> {
    public PlayerArgument(String name) {
        super(name, EntityArgument.player());
    }

    @Override
    public ServerPlayer cast(Object parsedArgument) {
        return ((ServerPlayer) parsedArgument);
    }

    @Override
    protected ServerPlayer parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return EntityArgument.getPlayer(ctx.getHandle(), name());
    }
}
