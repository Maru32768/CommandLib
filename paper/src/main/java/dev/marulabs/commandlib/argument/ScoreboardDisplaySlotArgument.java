package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
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
