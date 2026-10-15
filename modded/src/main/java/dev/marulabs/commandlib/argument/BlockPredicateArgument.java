package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import java.util.function.Predicate;

/**
 * A block predicate in the vanilla {@code block_predicate} syntax of {@code /execute if block}: a block
 * ({@code stone}), a block tag ({@code #minecraft:logs}), with block states ({@code oak_stairs[facing=east]}) and NBT
 * ({@code chest{Items:[]}}). The client parses it and suggests blocks, tags and states.
 *
 * <p>The value is vanilla's predicate; test a block with
 * {@code predicate.test(new BlockInWorld(level, pos, true))}, which loads its chunk when needed as
 * {@code /execute if block} does. An unknown block fails while parsing; an unknown tag fails while parsing from 1.19
 * and when the command runs before.
 */
public class BlockPredicateArgument extends Argument<Predicate<BlockInWorld>, BlockPredicateArgument> {
    public BlockPredicateArgument(String name) {
        //? if >=1.19.3 {
        super(name,
              net.minecraft.commands.arguments.blocks.BlockPredicateArgument.blockPredicate(BuiltInCommandBuildContext.INSTANCE));
        //?} else
        /*super(name, net.minecraft.commands.arguments.blocks.BlockPredicateArgument.blockPredicate());*/
    }

    @Override
    @SuppressWarnings("unchecked")
    public Predicate<BlockInWorld> cast(Object parsedArgument) {
        return (Predicate<BlockInWorld>) parsedArgument;
    }

    @Override
    protected Predicate<BlockInWorld> parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return net.minecraft.commands.arguments.blocks.BlockPredicateArgument.getBlockPredicate(ctx.getHandle(),
                                                                                                name());
    }
}
