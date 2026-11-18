package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentPlayers;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayersArgument extends Argument<List<Player>, PlayersArgument> {
    public PlayersArgument(String name) {
        super(name,
              NMSArgumentPlayers.create()
                                .argument());
    }

    @Override
    public List<Player> cast(Object parsedArgument) {
        return ((List<Player>) parsedArgument);
    }

    @Override
    protected List<Player> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSArgumentPlayers.create()
                                 .parse(ctx.getHandle(), name());
    }
}
