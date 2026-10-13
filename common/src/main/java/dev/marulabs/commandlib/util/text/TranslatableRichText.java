package dev.marulabs.commandlib.util.text;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A text the client translates, made with {@link RichText#translatable(String, RichText...)}.
 */
public final class TranslatableRichText extends RichText {
    private final String key;
    private final List<RichText> arguments;

    TranslatableRichText(String key, List<RichText> arguments, TextStyle style, List<RichText> children) {
        super(style, children);
        this.key = key;
        this.arguments = arguments;
    }

    @NotNull
    public String key() {
        return key;
    }

    /**
     * The texts that fill the placeholders of the translation.
     */
    @NotNull
    public List<RichText> arguments() {
        return arguments;
    }

    @Override
    RichText with(TextStyle style, List<RichText> children) {
        return new TranslatableRichText(key, arguments, style, children);
    }

    @Override
    String plainContent() {
        return key;
    }

    @Override
    void appendContent(StringBuilder builder) {
        builder.append("translatable=\"")
               .append(key)
               .append('"');
        if (!arguments.isEmpty()) {
            builder.append(", arguments=")
                   .append(arguments);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TranslatableRichText) || !super.equals(o)) {
            return false;
        }
        TranslatableRichText that = (TranslatableRichText) o;
        return key.equals(that.key) && arguments.equals(that.arguments);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * super.hashCode() + key.hashCode()) + arguments.hashCode();
    }
}
