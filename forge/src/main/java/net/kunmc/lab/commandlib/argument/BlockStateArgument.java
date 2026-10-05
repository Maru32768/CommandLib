package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.blocks.BlockInput;

public class BlockStateArgument extends Argument<BlockInput, BlockStateArgument> {
    public BlockStateArgument(String name) {
        //? if >=1.19.3 {
        super(name, net.minecraft.commands.arguments.blocks.BlockStateArgument.block(BuiltInCommandBuildContext.INSTANCE));
        //?} else
        /*super(name, net.minecraft.commands.arguments.blocks.BlockStateArgument.block());*/
    }

    @Override
    public BlockInput cast(Object parsedArgument) {
        return ((BlockInput) parsedArgument);
    }

    @Override
    protected BlockInput parseImpl(CommandContext ctx) throws ArgumentParseException {
        return net.minecraft.commands.arguments.blocks.BlockStateArgument.getBlock(ctx.getHandle(), name());
    }
}
