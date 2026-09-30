package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Set;

public class BlockStateArgument extends Argument<BlockInput, BlockStateArgument> {
    public BlockStateArgument(String name) {
        super(name, ResourceLocationArgument.id());
    }

    @Override
    public BlockInput cast(Object parsedArgument) {
        return ((BlockInput) parsedArgument);
    }

    @Override
    protected BlockInput parseImpl(CommandContext ctx) throws ArgumentParseException {
        return new BlockInput(BuiltInRegistries.BLOCK.get(ResourceLocationArgument.getId(ctx.getHandle(), name()))
                                                    .defaultBlockState(),
                              Set.of(),
                              null);
    }
}
