package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;

public class PlayerArgument extends Argument<ServerPlayerEntity, PlayerArgument> {
    public PlayerArgument(String name) {
        super(name, EntityArgument.player());
    }

    @Override
    public ServerPlayerEntity cast(Object parsedArgument) {
        return ((ServerPlayerEntity) parsedArgument);
    }

    @Override
    protected ServerPlayerEntity parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return EntityArgument.getPlayer(ctx.getHandle(), name());
    }
}
