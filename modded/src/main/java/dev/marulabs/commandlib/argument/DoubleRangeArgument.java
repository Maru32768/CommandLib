package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.RangeArgument;
import org.jetbrains.annotations.Nullable;

/**
 * A decimal range in the vanilla {@code float_range} syntax: {@code 0.5..10}, {@code ..5}, {@code 3.5..} or
 * {@code 5}. The client parses and highlights it like the {@code distance} of an entity selector.
 *
 * <p>The value is a closed {@link Range}, unbounded on a side the input leaves open: {@code 5} is {@code [5.0..5.0]}
 * and {@code 3.5..} is {@code [3.5..+∞)}. A minimum above the maximum is rejected while parsing.
 */
public class DoubleRangeArgument extends Argument<Range<Double>, DoubleRangeArgument> {
    public DoubleRangeArgument(String name) {
        super(name, RangeArgument.floatRange());
    }

    // The bounds are Float up to 1.16.5 and Double after. Going through the decimal string keeps 0.1f as 0.1.
    @Nullable
    private static Double toDouble(@Nullable Number value) {
        return value == null ? null : Double.valueOf(value.toString());
    }

    @Override
    public Range<Double> cast(Object parsedArgument) {
        return (Range<Double>) parsedArgument;
    }

    @Override
    protected Range<Double> parseImpl(CommandContext ctx) {
        // MinMaxBounds.Floats up to 1.16.5, MinMaxBounds.Doubles after.
        MinMaxBounds<?> bounds = ctx.getHandle()
                                    .getArgument(name(), MinMaxBounds.class);
        //? if >=1.20.2 {
        /*return IntegerRangeArgument.range(toDouble(bounds.min().orElse(null)), toDouble(bounds.max().orElse(null)));
        *///?} else
        return IntegerRangeArgument.range(toDouble(bounds.getMin()), toDouble(bounds.getMax()));
    }
}
