package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class CommonObjectArgument<T, C extends CommonCommandContext<?, ?>, SELF extends CommonObjectArgument<T, C, SELF>> extends CommonArgument<T, C, SELF> {
    private final Supplier<Map<String, ? extends T>> mapSupplier;

    public CommonObjectArgument(String name, Map<String, ? extends T> nameToObjectMap) {
        this(name, () -> nameToObjectMap);
    }

    public CommonObjectArgument(String name, Supplier<Map<String, ? extends T>> mapSupplier) {
        super(name, StringArgumentType.string());
        this.mapSupplier = Objects.requireNonNull(mapSupplier);
        addSuggestionAction(sb -> {
            mapSupplier.get()
                       .entrySet()
                       .stream()
                       .filter(x -> filter(sb.getContext()).test(x.getValue()))
                       .map(Map.Entry::getKey)
                       .filter(x -> sb.getLatestInput()
                                      .isEmpty() || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
                       .forEach(sb::suggest);
        });
    }

    @Override
    public final T cast(Object parsedArgument) {
        return ((T) parsedArgument);
    }

    @Override
    protected final T parseImpl(C ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        return mapSupplier.get()
                          .entrySet()
                          .stream()
                          .filter(x -> x.getKey()
                                        .equals(s))
                          .map(Map.Entry::getValue)
                          .findFirst()
                          .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(this.name(), ctx, s));
    }
}
