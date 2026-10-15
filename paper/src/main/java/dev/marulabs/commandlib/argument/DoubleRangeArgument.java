package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.range.DoubleRangeProvider;

/**
 * A decimal range in the vanilla {@code float_range} syntax: {@code 0.5..10}, {@code ..5}, {@code 3.5..} or
 * {@code 5}. The client parses and highlights it like the {@code distance} of an entity selector.
 *
 * <p>The value is a closed {@link Range}, unbounded on a side the input leaves open: {@code 5} is {@code [5.0..5.0]}
 * and {@code 3.5..} is {@code [3.5..+∞)}. A minimum above the maximum is rejected while parsing.
 */
@SuppressWarnings("UnstableApiUsage")
public class DoubleRangeArgument extends Argument<Range<Double>, DoubleRangeArgument> {
    public DoubleRangeArgument(String name) {
        super(name, ArgumentTypes.doubleRange());
    }

    @Override
    public Range<Double> cast(Object parsedArgument) {
        return (Range<Double>) parsedArgument;
    }

    @Override
    protected Range<Double> parseImpl(CommandContext ctx) {
        return ctx.getHandle()
                  .getArgument(name(), DoubleRangeProvider.class)
                  .range();
    }
}
