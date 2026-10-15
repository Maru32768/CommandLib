package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.world.level.GameType;
//? if <1.19.4 {
/*import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.util.StringUtil;

import java.util.Arrays;
*///?}

/**
 * A game mode by its name: {@code survival}, {@code creative}, {@code adventure} or {@code spectator}. From 1.19.4 it is
 * vanilla's {@code gamemode} argument, which the client validates and suggests; before, it is a word with the names
 * suggested.
 */
public class GameModeArgument extends Argument<GameType, GameModeArgument> {
    public GameModeArgument(String name) {
        //? if >=1.19.4 {
        super(name, net.minecraft.commands.arguments.GameModeArgument.gameMode());
        //?} else {
        /*super(name, StringArgumentType.word());
        addSuggestionProvider(sb -> Arrays.stream(GameType.values())
                                          .filter(x -> x.getId() >= 0)
                                          .map(GameType::getName)
                                          .filter(x -> sb.getLatestInput()
                                                         .isEmpty()
                                                  || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
                                          .forEach(sb::suggest));
        *///?}
    }

    @Override
    public GameType cast(Object parsedArgument) {
        return (GameType) parsedArgument;
    }

    @Override
    protected GameType parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        //? if >=1.19.4 {
        return net.minecraft.commands.arguments.GameModeArgument.getGameMode(ctx.getHandle(), name());
        //?} else {
        /*String value = StringArgumentType.getString(ctx.getHandle(), name());
        GameType gameType = GameType.byName(value, null);
        // NOT_SET of 1.16.5 has the id -1 and is not a game mode a sender can pick.
        if (gameType == null || gameType.getId() < 0) {
            throw ArgumentParseException.ofIncorrectInput(name(), ctx, value);
        }
        return gameType;
        *///?}
    }
}
