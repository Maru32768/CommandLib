package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Team;

public class TeamArgument extends Argument<Team, TeamArgument> {
    public TeamArgument(String name) {
        super(name,
              NMSArgumentScoreboardTeam.create()
                                       .argument());
    }

    @Override
    public Team cast(Object parsedArgument) {
        return ((Team) parsedArgument);
    }

    @Override
    protected Team parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        String teamName = NMSArgumentScoreboardTeam.create()
                                                   .parse(ctx.getHandle(), name())
                                                   .getName();
        Team team = Bukkit.getScoreboardManager()
                          .getMainScoreboard()
                          .getTeam(teamName);
        if (team == null) {
            // The NMS scoreboard knew the team, but it is not on the Bukkit main scoreboard (e.g. removed meanwhile).
            throw ArgumentParseException.ofIncorrectInput(name(), ctx, teamName);
        }
        return team;
    }
}
