package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentDimension;
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
