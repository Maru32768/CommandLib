package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.GameMode;

@SuppressWarnings("UnstableApiUsage")
public class GameModeArgument extends Argument<GameMode, GameModeArgument> {
    public GameModeArgument(String name) {
        super(name, ArgumentTypes.gameMode());
    }

    @Override
    public GameMode cast(Object parsedArgument) {
        return (GameMode) parsedArgument;
    }

    @Override
    protected GameMode parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), GameMode.class);
    }
}
