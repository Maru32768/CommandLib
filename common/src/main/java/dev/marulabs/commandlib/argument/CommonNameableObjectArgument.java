package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

public class CommonNameableObjectArgument<T extends Nameable, C extends CommonCommandContext<?, ?>, SELF extends CommonNameableObjectArgument<T, C, SELF>> extends CommonArgument<T, C, SELF> {
    private final Supplier<Collection<? extends T>> candidatesSupplier;

    public CommonNameableObjectArgument(String name, Collection<? extends T> candidates) {
        this(name, () -> candidates);
    }

    public CommonNameableObjectArgument(String name, Supplier<Collection<? extends T>> candidatesSupplier) {
        super(name, StringArgumentType.string());
        this.candidatesSupplier = Objects.requireNonNull(candidatesSupplier);
        addSuggestionAction(sb -> {
            candidatesSupplier.get()
                              .stream()
                              .filter(filter(sb.getContext()))
                              .map(Nameable::tabCompleteName)
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
        return candidatesSupplier.get()
                                 .stream()
                                 .filter(x -> x.tabCompleteName()
                                               .equals(s))
                                 .findFirst()
                                 .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(this.name(), ctx, s));
    }
}
