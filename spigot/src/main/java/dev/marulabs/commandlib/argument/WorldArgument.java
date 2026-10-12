package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentDimension;
import org.bukkit.World;

public class WorldArgument extends Argument<World, WorldArgument> {
    public WorldArgument(String name) {
        super(name,
              NMSArgumentDimension.create()
                                  .argument());
    }

    @Override
    public World cast(Object parsedArgument) {
        return (World) parsedArgument;
    }

    @Override
    protected World parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return NMSArgumentDimension.create()
                                   .parse(ctx.getHandle(), name());
    }
}
