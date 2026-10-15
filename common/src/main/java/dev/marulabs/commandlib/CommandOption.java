package dev.marulabs.commandlib;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.exception.OptionParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A typed option of a command, such as {@code -f}, {@code --force} or {@code --limit 10}. Create one with
 * {@link Options}, register it with {@code option(...)} on the command and read it with
 * {@link CommonCommandContext#getOption(CommandOption)}.
 *
 * @param <T> the type {@link CommonCommandContext#getOption(CommandOption)} returns
 * @param <C> the command context type
 */
public final class CommandOption<T, C extends CommonCommandContext<?, ?>> {
    static final String INTERNAL_NAME_PREFIX = "__commandlib_option_";

    private final String name;
    private final List<String> aliases = new ArrayList<>();
    // Null for an option without a short name.
    private final @Nullable Character shortName;
    private final T defaultValue;
    // Null for a flag, which takes no value.
    private final @Nullable ArgumentType<?> type;
    // Reads the value from the internal argument names of the occurrences, one name per occurrence.
    private final BiFunction<C, List<String>, T> reader;
    private final int maxOccurrences;
    private final List<OptionValidator<C>> validators = new ArrayList<>();
    private String description = "";
    private boolean required;

    /**
     * An option that takes a value of {@code type}, which {@code parser} reads from the context by the option's
     * internal argument name.
     */
    public CommandOption(@NotNull String name,
                         @Nullable Character shortName,
                         @NotNull T defaultValue,
                         @NotNull ArgumentType<?> type,
                         @NotNull BiFunction<C, String, T> parser) {
        this(name, shortName, defaultValue, Objects.requireNonNull(type), 1, readFirst(Objects.requireNonNull(parser)));
    }

    private CommandOption(String name,
                          @Nullable Character shortName,
                          T defaultValue,
                          @Nullable ArgumentType<?> type,
                          int maxOccurrences,
                          BiFunction<C, List<String>, T> reader) {
        this.name = validateName(name);
        if (shortName != null && !StringReader.isAllowedInUnquotedString(shortName)) {
            throw new IllegalArgumentException("Invalid option short name: " + shortName);
        }
        this.shortName = shortName;
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.type = type;
        if (maxOccurrences < 1) {
            throw new IllegalArgumentException("maxOccurrences must be at least 1: " + maxOccurrences);
        }
        this.maxOccurrences = maxOccurrences;
        this.reader = reader;
    }

    private static <C, T> BiFunction<C, List<String>, T> readFirst(BiFunction<C, String, T> parser) {
        return (ctx, names) -> parser.apply(ctx, names.get(0));
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is not expected to empty.");
        }
        // Brigadier matches "--name" as a literal, and the internal argument names append "#n" to it.
        if (!name.chars()
                 .allMatch(x -> StringReader.isAllowedInUnquotedString((char) x))) {
            throw new IllegalArgumentException("Invalid option name: " + name);
        }
        return name;
    }

    /**
     * Returns a flag, which takes no value and is {@code true} when given.
     */
    static <C extends CommonCommandContext<?, ?>> CommandOption<Boolean, C> flag(String name,
                                                                                 @Nullable Character shortName) {
        return new CommandOption<>(name, shortName, false, null, 1, (ctx, names) -> true);
    }

    /**
     * Returns a flag that counts how many times it is given, such as {@code -vvv} or {@code -v --verbose}, up to
     * {@code maxCount} times.
     */
    static <C extends CommonCommandContext<?, ?>> CommandOption<Integer, C> counter(String name,
                                                                                    @Nullable Character shortName,
                                                                                    int maxCount) {
        return new CommandOption<>(name, shortName, 0, null, maxCount, (ctx, names) -> names.size());
    }

    public String name() {
        return name;
    }

    /**
     * Adds long names that work like {@code --name}, such as {@code --yes} for {@code --force}.
     */
    public CommandOption<T, C> aliases(@NotNull String... aliases) {
        for (String alias : aliases) {
            validateName(alias);
            if (alias.equals(name) || this.aliases.contains(alias)) {
                throw new IllegalArgumentException("Duplicate option alias: " + alias);
            }
            this.aliases.add(alias);
        }
        return this;
    }

    /**
     * Returns the long names of this option, its name first and then its aliases.
     */
    public List<String> names() {
        List<String> names = new ArrayList<>();
        names.add(name);
        names.addAll(aliases);
        return names;
    }

    public CommandOption<T, C> description(@NotNull String description) {
        this.description = Objects.requireNonNull(description);
        return this;
    }

    public String description() {
        return description;
    }

    /**
     * Makes the command fail with a message when this option is not given. The default value is never returned.
     * Help, which runs where the command has no executor, does not need the option.
     */
    public CommandOption<T, C> required() {
        this.required = true;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    /**
     * Returns an option that can be given up to {@code maxCount} times, such as {@code --tag a --tag b}, and whose
     * value is the list of the given values in input order. The list is empty when the option is not given, so the
     * default value of this option is not used. Each further occurrence adds a level of option nodes to the command
     * tree, so keep {@code maxCount} small.
     * <p>
     * Call this before {@link #requires(CommandOption)} and the other modifiers, which the returned option does not
     * copy except for the aliases and the description.
     *
     * @throws IllegalStateException if this option is a flag
     */
    public CommandOption<List<T>, C> repeatable(int maxCount) {
        ArgumentType<?> valueType = type();
        BiFunction<C, List<String>, T> single = reader;
        CommandOption<List<T>, C> option = new CommandOption<>(name,
                                                               shortName,
                                                               List.of(),
                                                               valueType,
                                                               maxCount,
                                                               (ctx, names) -> names.stream()
                                                                                    .map(x -> single.apply(ctx,
                                                                                                           List.of(x)))
                                                                                    .collect(Collectors.toUnmodifiableList()));
        option.aliases.addAll(aliases);
        option.description = description;
        return option;
    }

    public CommandOption<T, C> requires(@NotNull CommandOption<?, ?> option) {
        Objects.requireNonNull(option);
        validators.add(ctx -> {
            if (!ctx.hasOption(option)) {
                throw new OptionParseException(Caption.OPTION_REQUIRES,
                                               Map.of("option", formatName(), "required", option.formatName()));
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
                throw new OptionParseException(Caption.OPTION_REQUIRES_VALUE,
                                               Map.of("option",
                                                      formatName(),
                                                      "required",
                                                      option.formatName(),
                                                      "value",
                                                      expectedDescription));
            }
        });
        return this;
    }

    @Nullable
    public Character shortName() {
        return shortName;
    }

    /**
     * Returns how many times this option can be given.
     */
    public int maxOccurrences() {
        return maxOccurrences;
    }

    T defaultValue() {
        return defaultValue;
    }

    boolean hasValue() {
        return type != null;
    }

    ArgumentType<?> type() {
        if (type == null) {
            throw new IllegalStateException(formatName() + " is a flag and takes no value.");
        }
        return type;
    }

    /**
     * Returns the name of the argument node that holds the value of the given occurrence, counted from 0.
     */
    String internalName(int occurrence) {
        return INTERNAL_NAME_PREFIX + name + (occurrence == 0 ? "" : "#" + (occurrence + 1));
    }

    T read(C ctx, List<String> occurrenceNames) {
        return reader.apply(ctx, occurrenceNames);
    }

    void validate(C ctx, boolean checkRequired) throws OptionParseException {
        if (!ctx.hasOption(this)) {
            if (checkRequired && required) {
                throw new OptionParseException(Caption.OPTION_MISSING, Map.of("option", formatName()));
            }
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
        void validate(C ctx) throws OptionParseException;
    }
}
