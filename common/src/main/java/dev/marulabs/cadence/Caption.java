package dev.marulabs.cadence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * The key of a text that {@link Captions} translates, with its English default. The built-in texts of Cadence are
 * the constants of this class; a plugin makes its own with {@link #of(String, String)}. Two captions are equal when
 * their keys are.
 * <p>
 * A text may hold placeholders such as {@code {option}}, which
 * {@link CommonCommandContext#caption(Caption, java.util.Map)} replaces.
 */
public final class Caption {
    // The English texts of the built-in captions, the same file translators start from.
    private static final Properties ENGLISH = load(Caption.class::getResourceAsStream,
                                                   Captions.BUILT_IN_BUNDLE + "_en.properties");
    private static final List<Caption> BUILT_IN = new ArrayList<>();

    /** {@code You do not have permission to execute this command.} */
    public static final Caption PERMISSION_DENIED = builtIn("cadence.permission.denied");
    /** {@code This command can only be executed by a player.} */
    public static final Caption REQUIRE_PLAYER = builtIn("cadence.prerequisite.player");
    /** {@code This command can only be executed from the console.} */
    public static final Caption REQUIRE_CONSOLE = builtIn("cadence.prerequisite.console");
    /** {@code Invalid value.}, the message of {@code new ArgumentValidationException()}. */
    public static final Caption INVALID_VALUE = builtIn("cadence.argument.invalid_value");
    /** {@code {value} is not a valid namespaced key} */
    public static final Caption INVALID_KEY = builtIn("cadence.argument.invalid_key");
    /** {@code {value} is neither a known player nor a valid UUID} */
    public static final Caption UNKNOWN_PLAYER_OR_UUID = builtIn("cadence.argument.unknown_player_or_uuid");
    /** {@code {value} is not a valid selector} */
    public static final Caption INVALID_SELECTOR = builtIn("cadence.argument.invalid_selector");
    /** {@code {value} is not a known loot table} */
    public static final Caption UNKNOWN_LOOT_TABLE = builtIn("cadence.argument.unknown_loot_table");
    /** {@code {value} is not a known attribute} */
    public static final Caption UNKNOWN_ATTRIBUTE = builtIn("cadence.argument.unknown_attribute");
    /** {@code {value} is not a known biome} */
    public static final Caption UNKNOWN_BIOME = builtIn("cadence.argument.unknown_biome");
    /** {@code {value} is not a known entity type} */
    public static final Caption UNKNOWN_ENTITY_TYPE = builtIn("cadence.argument.unknown_entity_type");
    /** {@code {value} is not a known sound} */
    public static final Caption UNKNOWN_SOUND = builtIn("cadence.argument.unknown_sound");
    /** {@code {value} is not a known registry entry} */
    public static final Caption UNKNOWN_REGISTRY_ENTRY = builtIn("cadence.argument.unknown_registry_entry");
    /** {@code {value} is not a display slot of this server} */
    public static final Caption UNKNOWN_DISPLAY_SLOT = builtIn("cadence.argument.unknown_display_slot");
    /** {@code {value} is not a color} */
    public static final Caption NOT_A_COLOR = builtIn("cadence.argument.not_a_color");
    /** {@code Tick count must not be less than {minimum}, found {ticks}} */
    public static final Caption TICK_COUNT_TOO_LOW = builtIn("cadence.argument.tick_count_too_low");
    /** {@code Missing required option {option}.} */
    public static final Caption OPTION_MISSING = builtIn("cadence.option.missing");
    /** {@code {option} requires {required}.} */
    public static final Caption OPTION_REQUIRES = builtIn("cadence.option.requires");
    /** {@code {option} requires {required} to be {value}.} */
    public static final Caption OPTION_REQUIRES_VALUE = builtIn("cadence.option.requires_value");
    /** {@code You must wait {remaining} before using this command again.} */
    public static final Caption COOLDOWN = builtIn("cadence.cooldown");
    /** {@code Run the command again within {timeout} to confirm.}, followed by {@link #CONFIRMATION_BUTTON}. */
    public static final Caption CONFIRMATION_REQUEST = builtIn("cadence.confirmation.request");
    /** {@code [Confirm]}, the button that runs the command again. */
    public static final Caption CONFIRMATION_BUTTON = builtIn("cadence.confirmation.button");
    /** {@code {value}h}, the hours of a duration such as {@code 1h 2m 3s}. */
    public static final Caption DURATION_HOURS = builtIn("cadence.duration.hours");
    /** {@code {value}m}, the minutes of a duration. */
    public static final Caption DURATION_MINUTES = builtIn("cadence.duration.minutes");
    /** {@code {value}s}, the seconds of a duration. */
    public static final Caption DURATION_SECONDS = builtIn("cadence.duration.seconds");
    /** {@code The command timed out.} */
    public static final Caption TIMEOUT = builtIn("cadence.execution.timeout");
    /**
     * {@code An unexpected error occurred trying to execute that command.} Without a text set for the sender's
     * locale, it is sent as vanilla's {@code command.failed}, which the client translates.
     */
    public static final Caption UNEXPECTED_ERROR = builtIn("cadence.execution.unexpected_error", "command.failed");
    /** {@code Check the console for details.} */
    public static final Caption SEE_CONSOLE = builtIn("cadence.execution.see_console");
    /** {@code Usage:}, the heading of the generated help. */
    public static final Caption HELP_USAGE = builtIn("cadence.help.usage");
    /** {@code Options:}, the heading of the options in the generated help. */
    public static final Caption HELP_OPTIONS = builtIn("cadence.help.options");
    /** {@code options}, the tag in {@code /scan [options] <target>}. */
    public static final Caption HELP_OPTIONS_TAG = builtIn("cadence.help.options_tag");
    /** {@code required}, the note of a required option. */
    public static final Caption HELP_REQUIRED = builtIn("cadence.help.required");
    /** {@code up to {count} times}, the note of an option that can be given more than once. */
    public static final Caption HELP_MAX_OCCURRENCES = builtIn("cadence.help.max_occurrences");
    /** {@code Examples:}, the heading of the examples in the generated help. */
    public static final Caption HELP_EXAMPLES = builtIn("cadence.help.examples");

    private final String key;
    private final String defaultText;
    // A vanilla translation key with the same meaning, sent while no text is set for the sender's locale.
    private final @Nullable String translationKey;

    private Caption(String key, String defaultText, @Nullable String translationKey) {
        this.key = key;
        this.defaultText = defaultText;
        this.translationKey = translationKey;
    }

    /**
     * Creates a caption with the given key and the text used when no translation is set for the sender's locale.
     */
    @NotNull
    public static Caption of(@NotNull String key, @NotNull String defaultText) {
        return new Caption(Objects.requireNonNull(key), Objects.requireNonNull(defaultText), null);
    }

    private static Caption builtIn(String key) {
        return builtIn(key, null);
    }

    private static Caption builtIn(String key, @Nullable String translationKey) {
        String text = ENGLISH.getProperty(key);
        if (text == null) {
            throw new IllegalStateException("The built-in bundle has no text for " + key);
        }
        Caption caption = new Caption(key, text, translationKey);
        BUILT_IN.add(caption);
        return caption;
    }

    /**
     * Returns the captions of Cadence's own texts.
     */
    @NotNull
    public static List<Caption> builtIns() {
        return Collections.unmodifiableList(BUILT_IN);
    }

    @NotNull
    public String key() {
        return key;
    }

    /**
     * Returns the English text used when no translation is set for the sender's locale.
     */
    @NotNull
    public String defaultText() {
        return defaultText;
    }

    @Nullable
    String translationKey() {
        return translationKey;
    }

    /**
     * Reads a properties file as UTF-8, or no properties when {@code open} returns null. The built-in bundle is opened
     * with {@code Caption.class::getResourceAsStream}, which also finds it when Cadence is part of an explicit JPMS
     * module, a user bundle with its class loader.
     */
    static Properties load(Function<String, InputStream> open, String resource) {
        Properties properties = new Properties();
        try (InputStream in = open.apply(resource)) {
            if (in != null) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Caption && key.equals(((Caption) o).key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }
}
