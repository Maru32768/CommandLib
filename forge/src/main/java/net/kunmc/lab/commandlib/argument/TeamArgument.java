package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.world.scores.PlayerTeam;

public class TeamArgument extends Argument<PlayerTeam, TeamArgument> {
    public TeamArgument(String name) {
        super(name, net.minecraft.commands.arguments.TeamArgument.team());
    }

    @Override
    public PlayerTeam cast(Object parsedArgument) {
        return ((PlayerTeam) parsedArgument);
    }

    @Override
    protected PlayerTeam parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return net.minecraft.commands.arguments.TeamArgument.getTeam(ctx.getHandle(), name());
    }
}
