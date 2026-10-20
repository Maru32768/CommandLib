package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentIntRange;

/**
 * An integer range in the vanilla {@code int_range} syntax: {@code 1..10}, {@code ..5}, {@code 3..} or {@code 5}. The
 * client parses and highlights it like the ranges of {@code /execute if score}.
 *
 * <p>The value is a closed {@link Range}, unbounded on a side the input leaves open: {@code 5} is {@code [5..5]} and
 * {@code 3..} is {@code [3..+∞)}. A minimum above the maximum is rejected while parsing.
 */
public class IntegerRangeArgument extends Argument<Range<Integer>, IntegerRangeArgument> {
    public IntegerRangeArgument(String name) {
        super(name,
              NMSArgumentIntRange.create()
                                 .argument());
    }

    @Override
    public Range<Integer> cast(Object parsedArgument) {
        return (Range<Integer>) parsedArgument;
    }

    @Override
    protected Range<Integer> parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return NMSArgumentIntRange.create()
                                  .parse(ctx.getHandle(), name());
    }
}
