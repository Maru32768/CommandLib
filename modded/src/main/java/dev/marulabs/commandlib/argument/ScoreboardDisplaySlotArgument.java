package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
//? if >=1.20.2
/*import net.minecraft.world.scores.DisplaySlot;*/

// A scoreboard display slot in the vanilla scoreboard_slot syntax, which the client validates and suggests: list,
// sidebar, below_name (belowName before 1.20.2) or sidebar.team.<color>. The value is vanilla's: a DisplaySlot from
// 1.20.2, and the slot index that Scoreboard#setDisplayObjective takes before.
//? if >=1.20.2 {
/*public class ScoreboardDisplaySlotArgument extends Argument<DisplaySlot, ScoreboardDisplaySlotArgument> {
*///?} else
public class ScoreboardDisplaySlotArgument extends Argument<Integer, ScoreboardDisplaySlotArgument> {
    public ScoreboardDisplaySlotArgument(String name) {
        super(name, ScoreboardSlotArgument.displaySlot());
    }

    //? if >=1.20.2 {
    /*@Override
    public DisplaySlot cast(Object parsedArgument) {
        return (DisplaySlot) parsedArgument;
    }

    @Override
    protected DisplaySlot parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return ScoreboardSlotArgument.getDisplaySlot(ctx.getHandle(), name());
    }
    *///?} else {
    @Override
    public Integer cast(Object parsedArgument) {
        return (Integer) parsedArgument;
    }

    @Override
    protected Integer parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return ScoreboardSlotArgument.getDisplaySlot(ctx.getHandle(), name());
    }
    //?}
}
