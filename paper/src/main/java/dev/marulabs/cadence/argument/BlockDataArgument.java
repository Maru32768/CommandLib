package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

@SuppressWarnings("UnstableApiUsage")
public class BlockDataArgument extends Argument<BlockData, BlockDataArgument> {
    public BlockDataArgument(String name) {
        super(name, ArgumentTypes.blockState());
    }

    @Override
    public BlockData cast(Object parsedArgument) {
        return (BlockData) parsedArgument;
    }

    @Override
    protected BlockData parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), BlockState.class)
                  .getBlockData();
    }
}
