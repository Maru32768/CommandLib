package dev.marulabs.commandlib.util.text;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A literal text, made with {@link RichText#text(String)}.
 */
public final class TextRichText extends RichText {
    private final String content;

    TextRichText(String content, TextStyle style, List<RichText> children) {
        super(style, children);
        this.content = content;
    }

    @NotNull
    public String content() {
        return content;
    }

    @Override
    RichText with(TextStyle style, List<RichText> children) {
        return new TextRichText(content, style, children);
    }

    @Override
    String plainContent() {
        return content;
    }

    @Override
    void appendContent(StringBuilder builder) {
        builder.append("text=\"")
               .append(content)
               .append('"');
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TextRichText && super.equals(o) && content.equals(((TextRichText) o).content);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + content.hashCode();
    }
}
