package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;
import java.util.function.Supplier;

public class TeamArgument extends Argument<Team, TeamArgument> {
    private Supplier<Scoreboard> scoreboardSupplier = () -> {
        return Bukkit.getScoreboardManager()
                     .getMainScoreboard();
    };

    public TeamArgument(String name) {
        super(name, StringArgumentType.word());

        addSuggestionProvider(sb -> {
            Scoreboard scoreboard = scoreboardSupplier.get();
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
    }

    /**
     * Looks teams up, for both parsing and suggestions, in the scoreboard the supplier returns at that time instead
     * of the main scoreboard.
     */
    public TeamArgument scoreboard(Supplier<Scoreboard> scoreboardSupplier) {
        this.scoreboardSupplier = Objects.requireNonNull(scoreboardSupplier);
        return this;
    }

    @Override
    public Team cast(Object parsedArgument) {
        return (Team) parsedArgument;
    }

    @Override
    protected Team parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        Scoreboard scoreboard = scoreboardSupplier.get();
        Team team = scoreboard == null ? null : scoreboard.getTeam(s);
        if (team == null) {
            throw ArgumentParseException.ofIncorrectInput(this.name(), ctx, s);
        }
        return team;
    }
}
