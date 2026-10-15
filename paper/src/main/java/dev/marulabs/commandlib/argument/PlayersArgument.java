package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PlayersArgument extends Argument<List<Player>, PlayersArgument> {
    public PlayersArgument(String name) {
        super(name, ArgumentTypes.players());
    }

    @Override
    public List<Player> cast(Object parsedArgument) {
        return (List<Player>) parsedArgument;
    }

    @Override
    protected List<Player> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = ctx.getHandle()
                                                     .getArgument(name(), PlayerSelectorArgumentResolver.class);
        return SelectorResults.requireAny(resolver.resolve(ctx.getHandle()
                                                              .getSource()),
                                          SelectorResults.NO_PLAYER);
    }
}
