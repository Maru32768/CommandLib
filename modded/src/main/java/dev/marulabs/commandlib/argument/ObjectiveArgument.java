package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.StringUtil;
import dev.marulabs.commandlib.util.text.RichText;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A scoreboard objective by its name, in vanilla's {@code objective} syntax with the server scoreboard's objectives
 * suggested. {@link #scoreboard(Supplier)} selects another scoreboard.
 */
public class ObjectiveArgument extends Argument<Objective, ObjectiveArgument> {
    // null keeps the vanilla lookup against the server scoreboard.
    @Nullable
    private Supplier<Scoreboard> scoreboardSupplier;

    public ObjectiveArgument(String name) {
        super(name, net.minecraft.commands.arguments.ObjectiveArgument.objective());
    }

    /**
     * Looks objectives up, for both parsing and suggestions, in the scoreboard the supplier returns at that time
     * instead of the server scoreboard.
     */
    public ObjectiveArgument scoreboard(Supplier<Scoreboard> scoreboardSupplier) {
        this.scoreboardSupplier = Objects.requireNonNull(scoreboardSupplier);
        // The vanilla suggestions list the server scoreboard's objectives.
        replaceDefaultSuggestions(sb -> {
            Scoreboard scoreboard = scoreboardSupplier.get();
            if (scoreboard == null) {
                return;
            }
            String input = sb.getLatestInput();
            scoreboard.getObjectives()
                      .stream()
                      .filter(filter(sb.getContext()))
                      .map(Objective::getName)
                      .filter(x -> input.isEmpty() || StringUtil.containsIgnoreCase(x, input))
                      .forEach(sb::suggest);
        });
        return this;
    }

    @Override
    public Objective cast(Object parsedArgument) {
        return ((Objective) parsedArgument);
    }

    @Override
    protected Objective parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        if (scoreboardSupplier == null) {
            return net.minecraft.commands.arguments.ObjectiveArgument.getObjective(ctx.getHandle(), name());
        }

        // The objective argument type parses to the raw objective name.
        String s = ctx.getHandle()
                      .getArgument(name(), String.class);
        Scoreboard scoreboard = scoreboardSupplier.get();
        Objective objective = scoreboard == null ? null : scoreboard.getObjective(s);
        if (objective == null) {
            throw new ArgumentParseException(x -> x.sendFailure(RichText.translatable("arguments.objective.notFound",
                                                                                      RichText.text(s))));
        }
        return objective;
    }
}
