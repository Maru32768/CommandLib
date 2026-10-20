package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentFloatRange;

/**
 * A decimal range in the vanilla {@code float_range} syntax: {@code 0.5..10}, {@code ..5}, {@code 3.5..} or
 * {@code 5}. The client parses and highlights it like the {@code distance} of an entity selector.
 *
 * <p>The value is a closed {@link Range}, unbounded on a side the input leaves open: {@code 5} is {@code [5.0..5.0]}
 * and {@code 3.5..} is {@code [3.5..+∞)}. A minimum above the maximum is rejected while parsing.
 */
public class DoubleRangeArgument extends Argument<Range<Double>, DoubleRangeArgument> {
    public DoubleRangeArgument(String name) {
        super(name,
              NMSArgumentFloatRange.create()
                                   .argument());
    }

    @Override
    public Range<Double> cast(Object parsedArgument) {
        return (Range<Double>) parsedArgument;
    }

    @Override
    protected Range<Double> parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return NMSArgumentFloatRange.create()
                                    .parse(ctx.getHandle(), name());
    }
}
