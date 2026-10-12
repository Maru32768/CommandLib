package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Objects;
import java.util.function.Supplier;

public class ObjectiveArgument extends Argument<Objective, ObjectiveArgument> {
    private Supplier<Scoreboard> scoreboardSupplier = () -> {
        return Bukkit.getScoreboardManager()
                     .getMainScoreboard();
    };

    public ObjectiveArgument(String name) {
        super(name, StringArgumentType.word());
        addSuggestionAction(sb -> {
            Scoreboard scoreboard = scoreboardSupplier.get();
            if (scoreboard == null) {
                return;
            }
            scoreboard.getObjectives()
                      .stream()
                      .map(Objective::getName)
                      .filter(x -> sb.getLatestInput()
                                     .isEmpty() || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
                      .forEach(sb::suggest);
        });
    }

    public ObjectiveArgument scoreboard(Supplier<Scoreboard> scoreboardSupplier) {
        this.scoreboardSupplier = Objects.requireNonNull(scoreboardSupplier);
        return this;
    }

    @Override
    public Objective cast(Object parsedArgument) {
        return (Objective) parsedArgument;
    }

    @Override
    protected Objective parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        String value = StringArgumentType.getString(ctx.getHandle(), name());
        Scoreboard scoreboard = scoreboardSupplier.get();
        Objective objective = scoreboard == null ? null : scoreboard.getObjective(value);
        if (objective == null) {
            throw new ArgumentParseException(x -> x.sendFailure(value + " is not a known objective"));
        }
        return objective;
    }
}
