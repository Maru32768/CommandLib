package dev.marulabs.commandlib;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class CommandOption<T, C extends CommonCommandContext<?, ?>> {
    static final String INTERNAL_NAME_PREFIX = "__commandlib_option_";

    private final String name;
    private final Character shortName;
    private final T defaultValue;
    private final ArgumentType<?> type;
    private final BiFunction<C, String, T> parser;
    private final String internalName;
    private final List<OptionValidator<C>> validators = new ArrayList<>();
    private String description = "";

    public CommandOption(@NotNull String name, @Nullable Character shortName, @NotNull T defaultValue) {
        this(name, shortName, defaultValue, null, null);
    }

    public CommandOption(@NotNull String name,
                         @Nullable Character shortName,
                         @NotNull T defaultValue,
                         @Nullable ArgumentType<?> type,
                         @Nullable BiFunction<C, String, T> parser) {
        this.name = Objects.requireNonNull(name);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is not expected to empty.");
        }
        this.shortName = shortName;
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.type = type;
        this.parser = parser;
        this.internalName = INTERNAL_NAME_PREFIX + name;
    }

    public String name() {
        return name;
    }

    public CommandOption<T, C> description(@NotNull String description) {
        this.description = Objects.requireNonNull(description);
        return this;
    }

    public String description() {
        return description;
    }

    public CommandOption<T, C> requires(@NotNull CommandOption<?, ?> option) {
        Objects.requireNonNull(option);
        validators.add(ctx -> {
            if (!ctx.hasOption(option)) {
                throw new ArgumentParseException(x -> {
                    x.sendFailure(formatName() + " requires " + option.formatName() + ".");
                });
            }
        });
        return this;
    }

    public <E> CommandOption<T, C> requires(@NotNull CommandOption<E, ?> option, @NotNull E expectedValue) {
        Objects.requireNonNull(option);
        Objects.requireNonNull(expectedValue);
        return requires(option, x -> Objects.equals(x, expectedValue), Objects.toString(expectedValue));
    }

    public <E> CommandOption<T, C> requires(@NotNull CommandOption<E, ?> option,
                                            @NotNull Predicate<? super E> predicate,
                                            @NotNull String expectedDescription) {
        Objects.requireNonNull(option);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(expectedDescription);
        validators.add(ctx -> {
            E value = ctx.getOption(option);
            if (!predicate.test(value)) {
                throw new ArgumentParseException(x -> {
                    x.sendFailure(formatName() + " requires " + option.formatName() + " to be " + expectedDescription + ".");
                });
            }
        });
        return this;
    }

    @Nullable
    public Character shortName() {
        return shortName;
    }

    T defaultValue() {
        return defaultValue;
    }

    boolean hasValue() {
        return type != null;
    }

    @Nullable ArgumentType<?> type() {
        return type;
    }

    String internalName() {
        return internalName;
    }

    T parse(C ctx) {
        return Objects.requireNonNull(parser)
                      .apply(ctx, internalName);
    }

    void validate(C ctx) throws ArgumentParseException {
        if (!ctx.hasOption(this)) {
            return;
        }

        for (OptionValidator<C> validator : validators) {
            validator.validate(ctx);
        }
    }

    String formatName() {
        return "--" + name;
    }

    @SuppressWarnings("unchecked")
    T cast(Object value) {
        return (T) value;
    }

    private interface OptionValidator<C extends CommonCommandContext<?, ?>> {
        void validate(C ctx) throws ArgumentParseException;
    }
}
