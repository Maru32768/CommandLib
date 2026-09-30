package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.util.math.BlockPos;

public class BlockPosArgument extends Argument<BlockPos, BlockPosArgument> {
    public BlockPosArgument(String name) {
        super(name, Vec3Argument.vec3());
    }

    @Override
    public BlockPos cast(Object parsedArgument) {
        return ((BlockPos) parsedArgument);
    }

    @Override
    protected BlockPos parseImpl(CommandContext ctx) throws ArgumentParseException {
        return Vec3Argument.getLocation(ctx.getHandle(), name())
                           .getBlockPos(ctx.getHandle()
                                           .getSource());
    }
}
