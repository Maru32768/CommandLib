package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayersArgument extends Argument<List<ServerPlayer>, PlayersArgument> {
    public PlayersArgument(String name) {
        super(name, EntityArgument.players());
    }

    @Override
    public List<ServerPlayer> cast(Object parsedArgument) {
        return ((List<ServerPlayer>) parsedArgument);
    }

    @Override
    protected List<ServerPlayer> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return new ArrayList<>(EntityArgument.getPlayers(ctx.getHandle(), name()));
    }
}
