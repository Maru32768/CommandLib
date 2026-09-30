package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class TeamArgument extends Argument<ScorePlayerTeam, TeamArgument> {
    public TeamArgument(String name) {
        super(name, net.minecraft.command.arguments.TeamArgument.team());
    }

    @Override
    public ScorePlayerTeam cast(Object parsedArgument) {
        return ((ScorePlayerTeam) parsedArgument);
    }

    @Override
    protected ScorePlayerTeam parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return net.minecraft.command.arguments.TeamArgument.getTeam(ctx.getHandle(), name());
    }
}
