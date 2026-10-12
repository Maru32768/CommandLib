package dev.marulabs.commandlib.argument;

import org.bukkit.scoreboard.DisplaySlot;

public class ScoreboardDisplaySlotArgument extends EnumArgument<DisplaySlot> {
    public ScoreboardDisplaySlotArgument(String name) {
        super(name, DisplaySlot.class);
    }
}
