package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
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
    protected Team parseImpl(CommandContext ctx) throws CommandSyntaxException {
        // The NMS lookup fails for an unknown team, and the Bukkit main scoreboard wraps the same server scoreboard.
        return Bukkit.getScoreboardManager()
                     .getMainScoreboard()
                     .getTeam(NMSArgumentScoreboardTeam.create()
                                                       .parse(ctx.getHandle(), name())
                                                       .getName());
    }
}
