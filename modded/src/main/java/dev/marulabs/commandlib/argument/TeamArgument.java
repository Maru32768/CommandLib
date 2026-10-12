package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Objects;
import java.util.function.Supplier;

public class TeamArgument extends Argument<PlayerTeam, TeamArgument> {
    // null keeps the vanilla lookup against the server scoreboard.
    private Supplier<Scoreboard> scoreboardSupplier;

    public TeamArgument(String name) {
        super(name, net.minecraft.commands.arguments.TeamArgument.team());
    }

    /**
     * Looks teams up, for both parsing and suggestions, in the scoreboard the supplier returns at that time instead
     * of the server scoreboard.
     */
    public TeamArgument scoreboard(Supplier<Scoreboard> scoreboardSupplier) {
        this.scoreboardSupplier = Objects.requireNonNull(scoreboardSupplier);
        // The vanilla suggestions list the server scoreboard's teams.
        replaceDefaultSuggestions(sb -> {
            Scoreboard scoreboard = this.scoreboardSupplier.get();
            if (scoreboard == null) {
                return;
            }
            String input = sb.getLatestInput();
            scoreboard.getPlayerTeams()
                      .stream()
                      .filter(filter(sb.getContext()))
                      .map(PlayerTeam::getName)
                      .filter(x -> input.isEmpty() || StringUtil.containsIgnoreCase(x, input))
                      .forEach(sb::suggest);
        });
        return this;
    }

    @Override
    public PlayerTeam cast(Object parsedArgument) {
        return ((PlayerTeam) parsedArgument);
    }

    @Override
    protected PlayerTeam parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        if (scoreboardSupplier == null) {
            return net.minecraft.commands.arguments.TeamArgument.getTeam(ctx.getHandle(), name());
        }

        // The team argument type parses to the raw team name.
        String s = ctx.getHandle()
                      .getArgument(name(), String.class);
        Scoreboard scoreboard = scoreboardSupplier.get();
        PlayerTeam team = scoreboard == null ? null : scoreboard.getPlayerTeam(s);
        if (team == null) {
            throw ArgumentParseException.ofIncorrectInput(this.name(), ctx, s);
        }
        return team;
    }
}
