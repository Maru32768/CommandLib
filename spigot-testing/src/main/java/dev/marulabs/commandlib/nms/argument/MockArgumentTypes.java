package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Argument types shared by the mock NMS arguments. They read the same tokens a server accepts so tests can use
 * production input such as {@code minecraft:stone} or {@code @s}.
 */
public final class MockArgumentTypes {
    private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    // No unit means ticks.
    private static final Map<String, Integer> TIME_UNITS = Map.of("d", 24000, "s", 20, "t", 1, "", 1);

    private MockArgumentTypes() {
    }

    /**
     * Reads everything up to the next whitespace.
     */
    public static ArgumentType<String> token() {
        return MockArgumentTypes::readToken;
    }

    /**
     * Reads a resource location such as {@code stone} or {@code minecraft:stone}, normalizes it to include the
     * namespace, and rejects it while parsing when {@code exists} does not accept it, like the registry-backed
     * arguments of a server.
     *
     * @param kind   used in the error message, for example {@code "item"}
     * @param exists receives the normalized key
     */
    public static ArgumentType<String> resourceKey(String kind, Predicate<String> exists) {
        return resourceKey(kind, exists, Collections::emptyList);
    }

    /**
     * Like {@link #resourceKey(String, Predicate)}, and suggests the keys that start with the input, with or without
     * the {@code minecraft} namespace, as a server suggests registry keys.
     *
     * @param keys the normalized keys to suggest
     */
    public static ArgumentType<String> resourceKey(String kind,
                                                   Predicate<String> exists,
                                                   Supplier<? extends Collection<String>> keys) {
        return new ArgumentType<String>() {
            @Override
            public String parse(StringReader reader) throws CommandSyntaxException {
                int start = reader.getCursor();
                String key = normalizeKey(readToken(reader));
                if (!RESOURCE_LOCATION.matcher(key)
                                      .matches()) {
                    reader.setCursor(start);
                    throw syntaxError("Invalid ID", reader);
                }
                if (!exists.test(key)) {
                    reader.setCursor(start);
                    throw syntaxError("Unknown " + kind + " '" + key + "'", reader);
                }
                return key;
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                                      SuggestionsBuilder builder) {
                String input = builder.getRemaining();
                for (String key : keys.get()) {
                    if (key.startsWith(input) || key.startsWith("minecraft:" + input)) {
                        builder.suggest(key);
                    }
                }
                return builder.buildFuture();
            }
        };
    }

    /**
     * Reads a vanilla range such as {@code 1..10}, {@code ..5}, {@code 3..} or {@code 5} into its minimum and maximum,
     * null on an open side, and rejects it while parsing like a server: when it is {@code ..}, a bound is not a
     * number, or the minimum is above the maximum.
     */
    public static <T extends Comparable<T>> ArgumentType<Object[]> bounds(Function<String, T> parseNumber) {
        return reader -> {
            int start = reader.getCursor();
            String token = readToken(reader);
            int separator = token.indexOf("..");
            String min = separator < 0 ? token : token.substring(0, separator);
            String max = separator < 0 ? token : token.substring(separator + 2);
            try {
                T lower = min.isEmpty() ? null : parseNumber.apply(min);
                T upper = max.isEmpty() ? null : parseNumber.apply(max);
                if (lower == null && upper == null) {
                    reader.setCursor(start);
                    throw syntaxError("Expected value or range of values", reader);
                }
                if (lower != null && upper != null && lower.compareTo(upper) > 0) {
                    reader.setCursor(start);
                    throw syntaxError("Min cannot be bigger than max", reader);
                }
                return new Object[]{lower, upper};
            } catch (NumberFormatException e) {
                reader.setCursor(start);
                throw syntaxError("Invalid number '" + token + "'", reader);
            }
        };
    }

    /**
     * Reads one JSON or SNBT value where the vanilla readers would end it: a quoted string, a value in balanced braces
     * or brackets whose quoted strings may hold any character, or else everything up to the next whitespace. The value
     * itself is not checked.
     */
    public static String readValue(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        if (!reader.canRead()
                || (reader.peek() != '{' && reader.peek() != '[' && !StringReader.isQuotedStringStart(reader.peek()))) {
            return readToken(reader);
        }
        int depth = 0;
        char quote = 0;
        while (reader.canRead()) {
            char c = reader.read();
            if (quote != 0) {
                if (c == '\\' && reader.canRead()) {
                    reader.skip();
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (StringReader.isQuotedStringStart(c)) {
                quote = c;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            }
            if (quote == 0 && depth == 0) {
                return reader.getString()
                             .substring(start, reader.getCursor());
            }
        }
        reader.setCursor(start);
        throw syntaxError("Unterminated value", reader);
    }

    /**
     * Reads one of {@code names}, rejecting other input while parsing with {@code <error> '<input>'}, and suggests the
     * names that start with the input, like the vanilla arguments over a fixed set of names.
     */
    public static ArgumentType<String> names(String error, Collection<String> names) {
        return new ArgumentType<String>() {
            @Override
            public String parse(StringReader reader) throws CommandSyntaxException {
                int start = reader.getCursor();
                String name = readToken(reader);
                if (!names.contains(name)) {
                    reader.setCursor(start);
                    throw syntaxError(error + " '" + name + "'", reader);
                }
                return name;
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                                      SuggestionsBuilder builder) {
                for (String name : names) {
                    if (name.startsWith(builder.getRemaining())) {
                        builder.suggest(name);
                    }
                }
                return builder.buildFuture();
            }
        };
    }

    /**
     * Reads a vanilla time such as {@code 20}, {@code 10s} or {@code 0.5d} into ticks, rejecting an unknown unit and
     * fewer ticks than {@code minimum} while parsing, and suggests the units after a number, like a server from 1.19.4.
     */
    public static ArgumentType<Integer> time(int minimum) {
        return new ArgumentType<Integer>() {
            @Override
            public Integer parse(StringReader reader) throws CommandSyntaxException {
                float amount = reader.readFloat();
                String unit = reader.readUnquotedString();
                int ticksPerUnit = TIME_UNITS.getOrDefault(unit, 0);
                if (ticksPerUnit == 0) {
                    throw syntaxError("Invalid unit", reader);
                }
                int ticks = Math.round(amount * ticksPerUnit);
                if (ticks < minimum) {
                    throw syntaxError("Tick count must not be less than " + minimum + ", found " + ticks, reader);
                }
                return ticks;
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                                      SuggestionsBuilder builder) {
                StringReader reader = new StringReader(builder.getRemaining());
                try {
                    reader.readFloat();
                } catch (CommandSyntaxException e) {
                    return builder.buildFuture();
                }
                SuggestionsBuilder units = builder.createOffset(builder.getStart() + reader.getCursor());
                for (String unit : TIME_UNITS.keySet()) {
                    if (!unit.isEmpty() && unit.startsWith(units.getRemaining())) {
                        units.suggest(unit);
                    }
                }
                return units.buildFuture();
            }
        };
    }

    /**
     * Adds the {@code minecraft} namespace when it is missing.
     */
    public static String normalizeKey(String key) {
        return key.contains(":") ? key : "minecraft:" + key;
    }

    /**
     * Returns the path of a namespaced key in the upper case used by Bukkit enum constants.
     */
    public static String bukkitName(String key) {
        String normalized = normalizeKey(key);
        return normalized.substring(normalized.indexOf(':') + 1)
                         .toUpperCase(Locale.ROOT);
    }

    /**
     * Creates the unchecked wrapper that {@code NMSArgument#parse} converts back into a
     * {@link CommandSyntaxException}, for failures detected while resolving an already parsed value.
     */
    public static UncheckedCommandSyntaxException resolveError(String message) {
        return new UncheckedCommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage(message)).create());
    }

    static CommandSyntaxException syntaxError(String message, StringReader reader) {
        return new SimpleCommandExceptionType(new LiteralMessage(message)).createWithContext(reader);
    }

    private static String readToken(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString()
                     .substring(start, reader.getCursor());
    }
}
