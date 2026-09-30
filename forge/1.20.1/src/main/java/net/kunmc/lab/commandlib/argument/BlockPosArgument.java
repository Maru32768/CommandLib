package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
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
        return net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(ctx.getHandle(), name());
    }
}
