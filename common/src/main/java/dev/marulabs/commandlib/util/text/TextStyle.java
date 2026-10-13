package dev.marulabs.commandlib.util.text;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * The style of a {@link RichText}, the same for every kind of text.
 */
final class TextStyle {
    static final int BOLD = 0;
    static final int ITALIC = 1;
    static final int UNDERLINED = 2;
    static final int STRIKETHROUGH = 3;
    static final int OBFUSCATED = 4;
    private static final String[] DECORATION_NAMES = {"bold", "italic", "underlined", "strikethrough", "obfuscated"};

    static final TextStyle EMPTY = new TextStyle(null, new Boolean[DECORATION_NAMES.length], null, null);

    final @Nullable Integer color;
    private final Boolean[] decorations;
    final @Nullable RichText hover;
    final @Nullable ClickAction click;

    private TextStyle(@Nullable Integer color,
                      Boolean[] decorations,
                      @Nullable RichText hover,
                      @Nullable ClickAction click) {
        this.color = color;
        this.decorations = decorations;
        this.hover = hover;
        this.click = click;
    }

    TextStyle color(int rgb) {
        return new TextStyle(rgb & 0xFFFFFF, decorations, hover, click);
    }

    TextStyle decoration(int index, boolean state) {
        Boolean[] newDecorations = decorations.clone();
        newDecorations[index] = state;
        return new TextStyle(color, newDecorations, hover, click);
    }

    @Nullable
    Boolean decoration(int index) {
        return decorations[index];
    }

    TextStyle hover(@Nullable RichText hover) {
        return new TextStyle(color, decorations, hover, click);
    }

    TextStyle click(@Nullable ClickAction click) {
        return new TextStyle(color, decorations, hover, click);
    }

    void appendTo(StringBuilder builder) {
        if (color != null) {
            builder.append(String.format(", color=#%06x", color));
        }
        for (int i = 0; i < DECORATION_NAMES.length; i++) {
            if (decorations[i] != null) {
                builder.append(", ")
                       .append(DECORATION_NAMES[i])
                       .append('=')
                       .append(decorations[i]);
            }
        }
        if (hover != null) {
            builder.append(", hover=")
                   .append(hover);
        }
        if (click != null) {
            builder.append(", click=")
                   .append(click);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextStyle)) {
            return false;
        }
        TextStyle that = (TextStyle) o;
        return Objects.equals(color, that.color) && Arrays.equals(decorations, that.decorations)
                && Objects.equals(hover, that.hover) && Objects.equals(click, that.click);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, Arrays.hashCode(decorations), hover, click);
    }
}
