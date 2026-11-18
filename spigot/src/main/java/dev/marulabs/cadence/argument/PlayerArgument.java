package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentPlayer;
import org.bukkit.entity.Player;

public class PlayerArgument extends Argument<Player, PlayerArgument> {
    public PlayerArgument(String name) {
        super(name,
              NMSArgumentPlayer.create()
                               .argument());
    }

    @Override
    public Player cast(Object parsedArgument) {
        return ((Player) parsedArgument);
    }

    @Override
    protected Player parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSArgumentPlayer.create()
                                .parse(ctx.getHandle(), name());
    }
}
