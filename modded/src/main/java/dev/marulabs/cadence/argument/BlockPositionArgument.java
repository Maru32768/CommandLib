package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;

/**
 * A block position in the vanilla {@code block_pos} syntax: {@code x y z} integers, relative ({@code ~}) or local
 * ({@code ^}) coordinates. The client completes it with the block it looks at.
 *
 * <p>Neither the chunk nor the world height is checked.
 */
public class BlockPositionArgument extends Argument<BlockPos, BlockPositionArgument> {
    public BlockPositionArgument(String name) {
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
