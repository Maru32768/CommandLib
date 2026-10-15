package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;

/**
 * A number of game ticks in the vanilla {@code time} syntax, read by Paper's {@link ArgumentTypes#time(int)}: a number,
 * decimals allowed, with one optional unit, {@code t} (ticks, the default), {@code s} (20 ticks) or {@code d} (a
 * Minecraft day, 24000 ticks), such as {@code 20}, {@code 10s} or {@code 0.5d}. The client parses it and suggests the
 * units.
 *
 * <p>Use {@link DurationArgument} for real time: it combines units ({@code 1h30m}) and its {@code d} is 24 hours.
 */
@SuppressWarnings("UnstableApiUsage")
public class TimeArgument extends Argument<Integer, TimeArgument> {
    public TimeArgument(String name) {
        this(name, 0);
    }

    /**
     * @param minimum the fewest ticks accepted
     */
    public TimeArgument(String name, int minimum) {
        super(name, ArgumentTypes.time(minimum));
    }

    @Override
    public Integer cast(Object parsedArgument) {
        return (Integer) parsedArgument;
    }

    @Override
    protected Integer parseImpl(CommandContext ctx) {
        return ctx.getHandle()
                  .getArgument(name(), Integer.class);
    }
}
