package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.command.arguments.BlockStateInput;

public class BlockStateArgument extends Argument<BlockStateInput, BlockStateArgument> {
    public BlockStateArgument(String name) {
        super(name, net.minecraft.command.arguments.BlockStateArgument.blockState());
    }

    @Override
    public BlockStateInput cast(Object parsedArgument) {
        return ((BlockStateInput) parsedArgument);
    }

    @Override
    protected BlockStateInput parseImpl(CommandContext ctx) throws ArgumentParseException {
        return net.minecraft.command.arguments.BlockStateArgument.getBlockState(ctx.getHandle(), name());
    }
}
