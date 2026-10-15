package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.proxy.Player;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;

/**
 * A player online on the proxy, by name, ignoring case. Suggests the names of the online players. The client sees a
 * word, since it knows only Brigadier's own argument types through the proxy.
 */
public class PlayerArgument extends Argument<Player, PlayerArgument> {
    public PlayerArgument(String name) {
        super(name, StringArgumentType.word());

        addSuggestionProvider(sb -> {
            sb.getContext()
              .getProxy()
              .getAllPlayers()
              .stream()
              .filter(filter(sb.getContext()))
              .map(Player::getUsername)
              .filter(x -> sb.getLatestInput()
                             .isEmpty()
                      || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
              .forEach(sb::suggest);
        });
    }

    @Override
    public Player cast(Object parsedArgument) {
        return (Player) parsedArgument;
    }

    @Override
    protected Player parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        return ctx.getProxy()
                  .getPlayer(s)
                  .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(name(), ctx, s));
    }
}
