package dev.marulabs.cadence.argument;

import org.bukkit.GameMode;

public class GameModeArgument extends EnumArgument<GameMode> {
    public GameModeArgument(String name) {
        super(name, GameMode.class);
    }
}
