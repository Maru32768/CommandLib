package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class PlayersArgument extends Argument<List<ServerPlayerEntity>, PlayersArgument> {
    public PlayersArgument(String name) {
        super(name, EntityArgument.players());
    }

    @Override
    public List<ServerPlayerEntity> cast(Object parsedArgument) {
        return ((List<ServerPlayerEntity>) parsedArgument);
    }

    @Override
    protected List<ServerPlayerEntity> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return new ArrayList<>(EntityArgument.getPlayers(ctx.getHandle(), name()));
    }
}
