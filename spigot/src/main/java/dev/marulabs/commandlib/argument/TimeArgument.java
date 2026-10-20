package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentTime;

import java.util.Map;

/**
 * A number of game ticks in the vanilla {@code time} syntax: a number, decimals allowed, with one optional unit,
 * {@code t} (ticks, the default), {@code s} (20 ticks) or {@code d} (a Minecraft day, 24000 ticks), such as {@code 20},
 * {@code 10s} or {@code 0.5d}. The client parses it and suggests the units.
 *
 * <p>Use {@link DurationArgument} for real time: it combines units ({@code 1h30m}) and its {@code d} is 24 hours.
 */
public class TimeArgument extends Argument<Integer, TimeArgument> {
    private final int minimum;

    public TimeArgument(String name) {
        this(name, 0);
    }

    /**
     * @param minimum the fewest ticks accepted
     */
    public TimeArgument(String name, int minimum) {
        super(name,
              NMSArgumentTime.create()
                             .argument(minimum));
        this.minimum = minimum;
    }

    @Override
    public Integer cast(Object parsedArgument) {
        return (Integer) parsedArgument;
    }

    @Override
    protected Integer parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        int ticks = NMSArgumentTime.create()
                                   .parse(ctx.getHandle(), name());
        // The type of servers before 1.19.4 has no minimum.
        if (ticks < minimum) {
            throw new ArgumentParseException(Caption.TICK_COUNT_TOO_LOW, Map.of("minimum", minimum, "ticks", ticks));
        }
        return ticks;
    }
}
