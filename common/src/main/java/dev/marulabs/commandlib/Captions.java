package dev.marulabs.commandlib;

import dev.marulabs.commandlib.util.text.RichText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The texts of {@link Caption}s for each locale, and how they are sent. {@link #create()} starts with CommandLib's
 * own translations; a command uses one with {@link CommonCommand#captions(Captions)}.
 * <p>
 * For a sender's locale, such as {@code ja_JP}, the text of a caption is looked up for {@code ja_JP}, then
 * {@code ja}, then the root locale, and at each step in this order: the texts given to {@link #set} and
 * {@link #load}, the bundles added with {@link #bundle(String, ClassLoader)} from the last added, and CommandLib's
 * own translations. Without any, the caption's {@link Caption#defaultText() English default} is used.
 * <p>
 * Set it up before the commands are registered; a lookup may run on any thread.
 */
public final class Captions {
    static final String BUILT_IN_BUNDLE = Captions.class.getPackageName()
                                                        .replace('.', '/')
            + "/lang/commandlib";
    static final Captions DEFAULT = create();

    // Locale suffix ("_ja_JP", "_ja", "") to key to text.
    private final Map<String, Map<String, String>> texts = new ConcurrentHashMap<>();
    // CommandLib's own translations first; later bundles win, so they are looked up from the end.
    private final List<Function<String, Properties>> bundles = new CopyOnWriteArrayList<>();
    private CaptionSender sender = (ctx, caption, message, send) -> send.accept(message);

    private Captions() {
    }

    /**
     * Creates a registry with CommandLib's own translations, which are English and Japanese.
     */
    @NotNull
    public static Captions create() {
        Captions captions = new Captions();
        // The English file holds the defaults, which come after every text, also one set for the root locale.
        captions.bundles.add(cached(suffix -> suffix.equals("_en") || suffix.startsWith("_en_") ? new Properties()
                : Caption.load(Captions.class.getClassLoader(), BUILT_IN_BUNDLE + suffix + ".properties")));
        return captions;
    }

    /**
     * Sets the text of {@code caption} for every locale that has no text of its own.
     */
    @NotNull
    public Captions set(@NotNull Caption caption, @NotNull String text) {
        return set(Locale.ROOT, caption, text);
    }

    /**
     * Sets the text of {@code caption} for {@code locale}, such as {@link Locale#JAPANESE} or {@link Locale#ROOT}.
     */
    @NotNull
    public Captions set(@NotNull Locale locale, @NotNull Caption caption, @NotNull String text) {
        Objects.requireNonNull(caption);
        Objects.requireNonNull(text);
        texts.computeIfAbsent(suffix(locale), x -> new ConcurrentHashMap<>())
             .put(caption.key(), text);
        return this;
    }

    /**
     * Sets the texts of {@code bundle}, keyed by {@link Caption#key()}, for {@code locale}. A file outside the
     * classpath, such as one in the plugin's data folder, loads with
     * {@code new PropertyResourceBundle(Files.newBufferedReader(path))}.
     */
    @NotNull
    public Captions load(@NotNull Locale locale, @NotNull ResourceBundle bundle) {
        Objects.requireNonNull(locale);
        for (String key : bundle.keySet()) {
            texts.computeIfAbsent(suffix(locale), x -> new ConcurrentHashMap<>())
                 .put(key, bundle.getString(key));
        }
        return this;
    }

    /**
     * Adds the properties files {@code baseName.properties}, {@code baseName_ja.properties},
     * {@code baseName_ja_JP.properties} and so on that {@code loader} finds, read as UTF-8, such as
     * {@code bundle("lang/messages", getClass().getClassLoader())} for {@code lang/messages_ja.properties} in the
     * plugin jar. A file is read the first time a sender of its locale needs it.
     */
    @NotNull
    public Captions bundle(@NotNull String baseName, @NotNull ClassLoader loader) {
        Objects.requireNonNull(baseName);
        Objects.requireNonNull(loader);
        bundles.add(cached(suffix -> Caption.load(loader, baseName + suffix + ".properties")));
        return this;
    }

    /**
     * Replaces how a caption is sent, such as to add a prefix, to color each caption, or to send a platform
     * component instead. See {@link CaptionSender}.
     */
    @NotNull
    public Captions sender(@NotNull CaptionSender sender) {
        this.sender = Objects.requireNonNull(sender);
        return this;
    }

    /**
     * Returns the text of {@code caption} for {@code locale}, with its placeholders left as they are.
     */
    @NotNull
    public String text(@NotNull Locale locale, @NotNull Caption caption) {
        String text = find(locale, caption, 0);
        return text != null ? text : caption.defaultText();
    }

    /**
     * Returns the text of {@code caption} for {@code locale} with each {@code {name}} replaced by the value of
     * {@code name} in {@code placeholders}. A placeholder without a value stays as it is.
     */
    @NotNull
    public String format(@NotNull Locale locale, @NotNull Caption caption, @NotNull Map<String, ?> placeholders) {
        return replace(text(locale, caption), placeholders);
    }

    /**
     * Sends {@code caption} to {@code ctx} through the {@link CaptionSender}, where {@code send} sends a message in
     * the built-in style, such as {@code ctx::sendFailure}.
     */
    void send(CommonCommandContext<?, ?> ctx,
              Caption caption,
              Map<String, ?> placeholders,
              @Nullable RichText suffix,
              Consumer<RichText> send) {
        String translationKey = caption.translationKey();
        // CommandLib's own texts do not count, so that the client translates it into every language.
        RichText message = translationKey != null && find(ctx.getLocale(), caption, 1) == null
                ? RichText.translatable(translationKey) : RichText.text(format(ctx.getLocale(), caption, placeholders));
        sender.send(ctx, caption, suffix == null ? message : message.append(suffix), send);
    }

    @Nullable
    private String find(Locale locale, Caption caption, int firstBundle) {
        for (String suffix : candidates(locale)) {
            Map<String, String> set = texts.get(suffix);
            String text = set == null ? null : set.get(caption.key());
            for (int i = bundles.size() - 1; text == null && i >= firstBundle; i--) {
                text = bundles.get(i)
                              .apply(suffix)
                              .getProperty(caption.key());
            }
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private static List<String> candidates(Locale locale) {
        List<String> result = new ArrayList<>(3);
        if (!locale.getLanguage()
                   .isEmpty()) {
            if (!locale.getCountry()
                       .isEmpty()) {
                result.add("_" + locale.getLanguage() + "_" + locale.getCountry());
            }
            result.add("_" + locale.getLanguage());
        }
        result.add("");
        return result;
    }

    private static String suffix(Locale locale) {
        return candidates(Objects.requireNonNull(locale)).get(0);
    }

    private static Function<String, Properties> cached(Function<String, Properties> loader) {
        Map<String, Properties> cache = new ConcurrentHashMap<>();
        return suffix -> cache.computeIfAbsent(suffix, loader);
    }

    static String replace(String text, Map<String, ?> placeholders) {
        if (placeholders.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int open = text.indexOf('{', i);
            int close = open < 0 ? -1 : text.indexOf('}', open);
            if (close < 0) {
                break;
            }
            String name = text.substring(open + 1, close);
            sb.append(text, i, open);
            if (placeholders.containsKey(name)) {
                sb.append(placeholders.get(name));
                i = close + 1;
            } else {
                // Not a placeholder: keep the brace and look for one after it.
                sb.append('{');
                i = open + 1;
            }
        }
        return sb.append(text, i, text.length())
                 .toString();
    }
}
