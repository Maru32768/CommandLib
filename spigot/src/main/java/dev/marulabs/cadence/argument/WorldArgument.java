package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentDimension;
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
