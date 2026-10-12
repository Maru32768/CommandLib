package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentPlayer;
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
