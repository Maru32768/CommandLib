package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.StringUtil;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class TeamArgument extends Argument<Team, TeamArgument> {
    // null keeps the vanilla lookup against the server scoreboard.
    private @Nullable Supplier<Scoreboard> scoreboardSupplier;

    public TeamArgument(String name) {
        super(name,
              NMSArgumentScoreboardTeam.create()
                                       .argument());
    }

    /**
     * Looks teams up, for both parsing and suggestions, in the scoreboard the supplier returns at that time instead
     * of the main scoreboard.
     */
    public TeamArgument scoreboard(Supplier<Scoreboard> scoreboardSupplier) {
        this.scoreboardSupplier = Objects.requireNonNull(scoreboardSupplier);
        // The vanilla suggestions list the server scoreboard's teams.
        replaceDefaultSuggestions(sb -> {
            Scoreboard scoreboard = Objects.requireNonNull(this.scoreboardSupplier)
                                           .get();
            if (scoreboard == null) {
                return;
            }
            String input = sb.getLatestInput();
            scoreboard.getTeams()
                      .stream()
                      .filter(filter(sb.getContext()))
                      .map(Team::getName)
                      .filter(x -> input.isEmpty() || StringUtil.containsIgnoreCase(x, input))
                      .forEach(sb::suggest);
        });
        return this;
    }

    @Override
    public Team cast(Object parsedArgument) {
        return ((Team) parsedArgument);
    }

    @Override
    protected Team parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        if (scoreboardSupplier == null) {
            // The NMS lookup fails for an unknown team, and the Bukkit main scoreboard wraps the same server scoreboard.
            return Bukkit.getScoreboardManager()
                         .getMainScoreboard()
                         .getTeam(NMSArgumentScoreboardTeam.create()
                                                           .parse(ctx.getHandle(), name())
                                                           .getName());
        }

        // The team argument type parses to the raw team name.
        String s = ctx.getHandle()
                      .getArgument(name(), String.class);
        Scoreboard scoreboard = scoreboardSupplier.get();
        Team team = scoreboard == null ? null : scoreboard.getTeam(s);
        if (team == null) {
            throw ArgumentParseException.ofIncorrectInput(this.name(), ctx, s);
        }
        return team;
    }
}
