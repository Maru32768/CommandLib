package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Argument types shared by the mock NMS arguments. They read the same tokens a server accepts so tests can use
 * production input such as {@code minecraft:stone} or {@code @s}.
 */
public final class MockArgumentTypes {
    private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

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
        return reader -> {
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
