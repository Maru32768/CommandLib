package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.core.BlockPos;

public class BlockPosArgument extends Argument<BlockPos, BlockPosArgument> {
    public BlockPosArgument(String name) {
        //? if >=1.20 {
        super(name, net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos());
        //?} else
        /*super(name, net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3());*/
    }

    @Override
    public BlockPos cast(Object parsedArgument) {
        return ((BlockPos) parsedArgument);
    }

    @Override
    protected BlockPos parseImpl(CommandContext ctx) throws ArgumentParseException {
        //? if >=1.20 {
        return net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(ctx.getHandle(), name());
        //?} else {
        /*return net.minecraft.commands.arguments.coordinates.Vec3Argument.getCoordinates(ctx.getHandle(), name())
                                                                        .getBlockPos(ctx.getHandle()
                                                                                        .getSource());
        *///?}
    }
}
