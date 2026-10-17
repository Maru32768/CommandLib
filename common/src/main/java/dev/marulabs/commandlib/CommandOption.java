package dev.marulabs.commandlib;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.exception.OptionParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

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
    // Null for an option of an argument, whose default the argument gives or which is required.
    private final @Nullable T defaultValue;
    // Null for a flag, which takes no value.
    private final @Nullable ArgumentType<?> type;
    // Reads the value from the internal argument names of the occurrences, one name per occurrence.
    private final Reader<C, T> reader;
    // The argument of an option made with Options.of, or null.
    private @Nullable CommonArgument<T, C, ?> argument;
    // The argument of the option a repeatable option was made from, for its suggestions.
    private @Nullable CommonArgument<?, C, ?> suggestionArgument;
    private final int maxOccurrences;
    private final List<OptionValidator<C>> validators = new ArrayList<>();
    // The options that requires(...) names, which must be options of the same command.
    private final List<CommandOption<?, ?>> requiredOptions = new ArrayList<>();
    private String description = "";
    private boolean required;
    // Whether required() was called, as opposed to an option of an argument that is not optional.
    private boolean requiredByCall;
    // Whether the option needs a permission; the node is generated unless permissionNode is set.
    private boolean restricted;
    // Set when a command registers the option; getOption rejects an option no command has, such as a repeatable copy.
    boolean registered;
    private @Nullable String permissionNode;
    // Null to take the default of the command.
    private @Nullable DefaultPermission defaultPermission;

    // An option that takes a value of type, which parser reads from the context by the option's internal argument
    // name. Users make options with Options, which keeps that name internal.
    CommandOption(String name,
                  @Nullable Character shortName,
                  T defaultValue,
                  ArgumentType<?> type,
                  BiFunction<C, String, T> parser) {
        this(name,
             shortName,
             Objects.requireNonNull(defaultValue),
             Objects.requireNonNull(type),
             1,
             readFirst(Objects.requireNonNull(parser)));
    }

    private CommandOption(String name,
                          @Nullable Character shortName,
                          @Nullable T defaultValue,
                          @Nullable ArgumentType<?> type,
                          int maxOccurrences,
                          Reader<C, T> reader) {
        this.name = validateName(name);
        // "-" would make the token "--", and combined tokens such as "--f" that collide with long names.
        if (shortName != null && (shortName == '-' || !StringReader.isAllowedInUnquotedString(shortName))) {
            throw new IllegalArgumentException("Invalid option short name: " + shortName);
        }
        this.shortName = shortName;
        this.defaultValue = defaultValue;
        this.type = type;
        if (maxOccurrences < 1) {
            throw new IllegalArgumentException("maxOccurrences must be at least 1: " + maxOccurrences);
        }
        this.maxOccurrences = maxOccurrences;
        this.reader = reader;
    }

    private static <C, T> Reader<C, T> readFirst(BiFunction<C, String, T> parser) {
        return (ctx, names) -> parser.apply(ctx, names.get(0));
    }

    /**
     * Returns an option whose value {@code argument} parses, with the argument's type, suggestions, validator and
     * transformer. When the option is not given, the default of an optional argument applies; an option of an
     * argument that is not optional is required.
     */
    static <T, C extends CommonCommandContext<?, ?>> CommandOption<T, C> of(String name,
                                                                            @Nullable Character shortName,
                                                                            CommonArgument<T, C, ?> argument) {
        Objects.requireNonNull(argument);
        if (argument instanceof VariadicArgument) {
            throw new IllegalArgumentException("An option takes one value; use repeatable(max) instead of the variadic argument "
                    + argument.name());
        }
        CommandOption<T, C> option = new CommandOption<>(name,
                                                         shortName,
                                                         null,
                                                         argument.type(),
                                                         1,
                                                         (ctx, names) -> argument.parseNode(ctx, names.get(0)));
        option.argument = argument;
        option.required = !argument.isOptional();
        return option;
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

    /**
     * Checks that no two of the options of command {@code commandName} share a long name or a short name.
     */
    static void checkDistinctNames(List<? extends CommandOption<?, ?>> options, String commandName) {
        Set<String> names = new HashSet<>();
        Set<Character> shortNames = new HashSet<>();
        for (CommandOption<?, ?> option : options) {
            for (String name : option.names()) {
                if (!names.add(name)) {
                    throw new IllegalArgumentException("Duplicate option name --" + name + " in command '" + commandName
                            + "'");
                }
            }
            if (option.shortName != null && !shortNames.add(option.shortName)) {
                throw new IllegalArgumentException("Duplicate option short name -" + option.shortName + " in command '"
                        + commandName + "'");
            }
        }
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
        this.requiredByCall = true;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    /**
     * Makes the option need the permission {@code <command node>.option.<name>}, such as
     * {@code myplugin.command.ban.option.silent}, with the given default. A sender without it neither sees nor can give
     * the option, and the node is registered and exported with the command's nodes.
     */
    public CommandOption<T, C> permission(@NotNull DefaultPermission defaultPermission) {
        this.restricted = true;
        this.defaultPermission = Objects.requireNonNull(defaultPermission);
        return this;
    }

    /**
     * Same as {@link #permission(DefaultPermission)} with this exact node, without the prefix, and the default of the
     * command.
     */
    public CommandOption<T, C> permission(@NotNull String node) {
        this.restricted = true;
        this.permissionNode = Objects.requireNonNull(node);
        return this;
    }

    /**
     * Same as {@link #permission(DefaultPermission)} with this exact node, without the prefix.
     */
    public CommandOption<T, C> permission(@NotNull String node, @NotNull DefaultPermission defaultPermission) {
        permission(node);
        this.defaultPermission = Objects.requireNonNull(defaultPermission);
        return this;
    }

    /**
     * The node the option needs on {@code command}, or null when it needs none.
     */
    @Nullable
    String permissionName(CommonCommand<?, ?> command, String prefix) {
        if (!restricted) {
            return null;
        }
        if (permissionNode != null) {
            return permissionNode;
        }
        return prefix + "." + command.permissionNameWithoutPrefix() + ".option." + name;
    }

    @Nullable
    PermissionConfig permissionConfig(CommonCommand<?, ?> command, String prefix) {
        String node = permissionName(command, prefix);
        if (node == null) {
            return null;
        }
        return new PermissionConfig(node,
                                    defaultPermission != null ? defaultPermission
                                            : command.effectiveDefaultPermission(),
                                    "");
    }

    /**
     * Returns an option that can be given up to {@code maxCount} times, such as {@code --tag a --tag b}, and whose
     * value is the list of the given values in input order. The list is empty when the option is not given, so the
     * default value of this option is not used. Each further occurrence adds a level of option nodes to the command
     * tree, so keep {@code maxCount} small.
     * <p>
     * The returned option keeps the aliases, the description, the permission, {@link #required()} and the
     * {@code requires(...)} dependencies; after {@link #required()} it needs at least one occurrence. An option of a
     * required argument ({@link Options#of}) becomes optional and gives an empty list.
     * <p>
     * The returned option is a new one, so register it: {@code option(Options.string("tag", "").repeatable(3))}.
     *
     * @throws IllegalStateException if this option is a flag
     */
    public CommandOption<List<T>, C> repeatable(int maxCount) {
        ArgumentType<?> valueType = type();
        Reader<C, T> single = reader;
        CommandOption<List<T>, C> option = new CommandOption<>(name,
                                                               shortName,
                                                               List.of(),
                                                               valueType,
                                                               maxCount,
                                                               (ctx, names) -> {
                                                                   List<T> values = new ArrayList<>();
                                                                   for (String x : names) {
                                                                       values.add(single.read(ctx, List.of(x)));
                                                                   }
                                                                   return List.copyOf(values);
                                                               });
        option.suggestionArgument = suggestionArgument();
        option.restricted = restricted;
        option.permissionNode = permissionNode;
        option.defaultPermission = defaultPermission;
        option.aliases.addAll(aliases);
        option.description = description;
        // A repeatable option of a required argument gives an empty list when it is not given; required() is kept.
        option.required = requiredByCall;
        option.requiredByCall = requiredByCall;
        option.validators.addAll(validators);
        option.requiredOptions.addAll(requiredOptions);
        return option;
    }

    public CommandOption<T, C> requires(@NotNull CommandOption<?, ?> option) {
        Objects.requireNonNull(option);
        requiredOptions.add(option);
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
        requiredOptions.add(option);
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

    /**
     * The default value, or null for an option of an argument.
     */
    @Nullable
    T defaultValue() {
        return defaultValue;
    }

    /**
     * The default of an option of an optional argument, computed for the context, or null for any other option.
     */
    @Nullable
    T defaultValue(C ctx) throws CommandPrerequisiteException {
        CommonArgument<T, C, ?> argument = this.argument;
        return argument != null && argument.isOptional() ? argument.defaultValue(ctx) : null;
    }

    /**
     * The argument whose suggestions the value of this option takes, or null to take those of its type.
     */
    @Nullable
    CommonArgument<?, C, ?> suggestionArgument() {
        return argument != null ? argument : suggestionArgument;
    }

    /**
     * Returns whether this option takes a value, as {@code --limit <limit>} does, instead of being a flag.
     */
    public boolean hasValue() {
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

    T read(C ctx, List<String> occurrenceNames) throws ArgumentParseException {
        return reader.read(ctx, occurrenceNames);
    }

    List<CommandOption<?, ?>> requiredOptions() {
        return requiredOptions;
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

    private interface Reader<C, T> {
        T read(C ctx, List<String> occurrenceNames) throws ArgumentParseException;
    }
}
