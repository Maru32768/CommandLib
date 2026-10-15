package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentBlockPredicate;
import org.bukkit.block.Block;

import java.util.function.Predicate;

/**
 * A block predicate in the vanilla {@code block_predicate} syntax of {@code /execute if block}: a block
 * ({@code stone}), a block tag ({@code #minecraft:logs}), with block states ({@code oak_stairs[facing=east]}) and NBT
 * ({@code chest{Items:[]}}). The client parses it and suggests blocks, tags and states.
 *
 * <p>The value tests a block in its world as the server does, loading its chunk when needed. An unknown block fails
 * while parsing; an unknown tag fails while parsing from 1.19 and when the command runs before.
 */
public class BlockPredicateArgument extends Argument<Predicate<Block>, BlockPredicateArgument> {
    public BlockPredicateArgument(String name) {
        super(name,
              NMSArgumentBlockPredicate.create()
                                       .argument());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Predicate<Block> cast(Object parsedArgument) {
        return (Predicate<Block>) parsedArgument;
    }

    @Override
    protected Predicate<Block> parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return NMSArgumentBlockPredicate.create()
                                        .parse(ctx.getHandle(), name());
    }
}
