package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.scoreboard.DisplaySlot;

@SuppressWarnings("UnstableApiUsage")
public class ScoreboardDisplaySlotArgument extends Argument<DisplaySlot, ScoreboardDisplaySlotArgument> {
    public ScoreboardDisplaySlotArgument(String name) {
        super(name, ArgumentTypes.scoreboardDisplaySlot());
    }

    @Override
    public DisplaySlot cast(Object parsedArgument) {
        return (DisplaySlot) parsedArgument;
    }

    @Override
    protected DisplaySlot parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), DisplaySlot.class);
    }
}
