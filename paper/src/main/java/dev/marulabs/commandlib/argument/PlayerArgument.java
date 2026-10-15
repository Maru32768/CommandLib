package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;


@SuppressWarnings("UnstableApiUsage")
public class PlayerArgument extends Argument<Player, PlayerArgument> {
    public PlayerArgument(String name) {
        super(name, ArgumentTypes.player());
    }

    @Override
    public Player cast(Object parsedArgument) {
        return (Player) parsedArgument;
    }

    @Override
    protected Player parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = ctx.getHandle()
                                                     .getArgument(name(), PlayerSelectorArgumentResolver.class);
        // Paper's player() resolver throws vanilla's error itself; this covers a resolver that returns no player.
        return SelectorResults.requireAny(resolver.resolve(ctx.getHandle()
                                                              .getSource()),
                                          SelectorResults.NO_PLAYER)
                              .get(0);
    }
}
