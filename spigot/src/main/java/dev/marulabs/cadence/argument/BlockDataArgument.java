package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentTile;
import dev.marulabs.cadence.internal.nms.world.NMSCraftBlockData;
import org.bukkit.block.data.BlockData;

public class BlockDataArgument extends Argument<BlockData, BlockDataArgument> {
    public BlockDataArgument(String name) {
        super(name,
              NMSArgumentTile.create()
                             .argument());
    }

    @Override
    public BlockData cast(Object parsedArgument) {
        return ((BlockData) parsedArgument);
    }

    @Override
    protected BlockData parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSCraftBlockData.create()
                                .createData(NMSArgumentTile.create()
                                                           .parse(ctx.getHandle(), name())
                                                           .getBlockData());
    }
}
