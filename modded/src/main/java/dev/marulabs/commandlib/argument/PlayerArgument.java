package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
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
