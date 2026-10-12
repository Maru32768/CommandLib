package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;

public class BlockPosArgument extends Argument<BlockPos, BlockPosArgument> {
    public BlockPosArgument(String name) {
        super(name, net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos());
    }

    @Override
    public BlockPos cast(Object parsedArgument) {
        return ((BlockPos) parsedArgument);
    }

    @Override
    protected BlockPos parseImpl(CommandContext ctx) throws ArgumentParseException {
        // Same as vanilla BlockPosArgument#getBlockPos, which is not available on every version.
        // It does not require the position to be loaded or inside the world bounds.
        return ctx.getHandle()
                  .getArgument(name(), Coordinates.class)
                  .getBlockPos(ctx.getHandle()
                                  .getSource());
    }
}
