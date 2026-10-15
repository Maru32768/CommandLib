package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Duration} written as one or more non-negative integers, each followed by a unit, which add up:
 * {@code 1h30m}, {@code 20t}, {@code 5s}, {@code 2d}. Each unit appears at most once, from the largest to the
 * smallest, so {@code 1m1m} and {@code 30s1h} are rejected. A number without a unit is rejected as well.
 *
 * <table>
 * <caption>Units</caption>
 * <tr><th>Unit</th><th>Length</th></tr>
 * <tr><td>{@code t}</td><td>a tick, 50 milliseconds</td></tr>
 * <tr><td>{@code s}</td><td>a second</td></tr>
 * <tr><td>{@code m}</td><td>a minute</td></tr>
 * <tr><td>{@code h}</td><td>an hour</td></tr>
 * <tr><td>{@code d}</td><td>a day, 24 hours</td></tr>
 * </table>
 *
 * <p>Every unit is real time. A tick is the 50 milliseconds a server running at 20 TPS spends on it, and {@code d} is a
 * real day. Vanilla's {@code time} argument instead counts game ticks and means the 24000 ticks of a Minecraft day
 * (20 minutes) by {@code d}. Convert with {@code duration.toMillis() / 50} to schedule game ticks.
 *
 * <p>The client sees a plain word, so the input is checked when the command runs. Typing a number suggests it with
 * each unit.
 */
public class CommonDurationArgument<C extends CommonCommandContext<?, ?>, SELF extends CommonDurationArgument<C, SELF>> extends CommonArgument<Duration, C, SELF> {
    // Each unit at most once, from the largest, so that a typo such as 1m1m or 30s1h is rejected.
    private static final Pattern FORMAT = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?(?:(\\d+)t)?");
    private static final String[][] UNITS = {{"d", "days"}, {"h", "hours"}, {"m", "minutes"}, {"s", "seconds"}, {"t", "ticks"}};
    private static final Duration[] UNIT_LENGTHS = {Duration.ofDays(1), Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofSeconds(1), Duration.ofMillis(50)};

    public CommonDurationArgument(String name) {
        super(name, StringArgumentType.word());
        addSuggestionProvider(sb -> {
            String input = sb.getLatestInput();
            if (input.isEmpty() || !Character.isDigit(input.charAt(input.length() - 1))) {
                return;
            }
            for (String[] unit : UNITS) {
                if (parse(input + unit[0]) != null) {
                    sb.suggest(input + unit[0], unit[1]);
                }
            }
        });
    }

    // Returns the duration the input stands for, or null when it is malformed or overflows a Duration.
    @Nullable
    static Duration parse(String input) {
        Matcher matcher = FORMAT.matcher(input);
        if (input.isEmpty() || !matcher.matches()) {
            return null;
        }
        Duration total = Duration.ZERO;
        try {
            for (int i = 0; i < UNITS.length; i++) {
                String amount = matcher.group(i + 1);
                if (amount != null) {
                    total = total.plus(UNIT_LENGTHS[i].multipliedBy(Long.parseLong(amount)));
                }
            }
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
        return total;
    }

    @Override
    public final Duration cast(Object parsedArgument) {
        return (Duration) parsedArgument;
    }

    @Override
    protected final Duration parseImpl(C ctx) throws ArgumentParseException {
        String input = StringArgumentType.getString(ctx.getHandle(), name());
        Duration duration = parse(input);
        if (duration == null) {
            throw ArgumentParseException.ofIncorrectInput(name(), ctx, input);
        }
        return duration;
    }
}
