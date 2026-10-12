package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTile;
import dev.marulabs.commandlib.util.nms.world.NMSCraftBlockData;
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
