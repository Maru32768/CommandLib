package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.RangeArgument;
import org.jetbrains.annotations.Nullable;

/**
 * An integer range in the vanilla {@code int_range} syntax: {@code 1..10}, {@code ..5}, {@code 3..} or {@code 5}. The
 * client parses and highlights it like the ranges of {@code /execute if score}.
 *
 * <p>The value is a closed {@link Range}, unbounded on a side the input leaves open: {@code 5} is {@code [5..5]} and
 * {@code 3..} is {@code [3..+∞)}. A minimum above the maximum is rejected while parsing.
 */
public class IntegerRangeArgument extends Argument<Range<Integer>, IntegerRangeArgument> {
    public IntegerRangeArgument(String name) {
        super(name, RangeArgument.intRange());
    }

    /**
     * Converts the bounds of a vanilla range, where null means unbounded, to a closed range.
     */
    static <C extends Comparable<?>> Range<C> range(@Nullable C min, @Nullable C max) {
        if (min == null) {
            return max == null ? Range.all() : Range.atMost(max);
        }
        return max == null ? Range.atLeast(min) : Range.closed(min, max);
    }

    @Override
    public Range<Integer> cast(Object parsedArgument) {
        return (Range<Integer>) parsedArgument;
    }

    @Override
    protected Range<Integer> parseImpl(CommandContext ctx) {
        MinMaxBounds.Ints bounds = ctx.getHandle()
                                      .getArgument(name(), MinMaxBounds.Ints.class);
        //? if >=1.20.2 {
        /*return range(bounds.min().orElse(null), bounds.max().orElse(null));
        *///?} else
        return range(bounds.getMin(), bounds.getMax());
    }
}
