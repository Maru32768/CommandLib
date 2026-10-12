package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class CommonEnumArgument<T extends Enum<T>, C extends CommonCommandContext<?, ?>, SELF extends CommonEnumArgument<T, C, SELF>> extends CommonArgument<T, C, SELF> {
    private final Class<T> clazz;

    public CommonEnumArgument(String name, Class<T> clazz) {
        super(name, StringArgumentType.word());
        this.clazz = Objects.requireNonNull(clazz);
        addSuggestionAction(sb -> {
            Arrays.stream(clazz.getEnumConstants())
                  .filter(filter(sb.getContext()))
                  .map(Enum::name)
                  .map(x -> x.toLowerCase(Locale.ROOT))
                  .filter(x -> sb.getLatestInput()
                                 .isEmpty()
                          || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
                  .forEach(sb::suggest);
        });
    }

    @Override
    public final T cast(Object parsedArgument) {
        return clazz.cast(parsedArgument);
    }

    @Override
    protected final T parseImpl(C ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        return Arrays.stream(clazz.getEnumConstants())
                     .filter(x -> x.name()
                                   .equalsIgnoreCase(s))
                     .findFirst()
                     .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(this.name(), ctx, s));
    }
}
