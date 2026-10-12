package net.kunmc.lab.commandlib.util.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A platform-independent, immutable chat message: a literal text or a translation key, with a color, decorations, a
 * hover text, a click action, and children.
 * <p>
 * Send it with {@code ctx.sendMessage(text)} on any platform. Every method that changes something returns a new
 * instance. Children inherit the style of their parent unless they set their own, as in Minecraft.
 * <p>
 * Decorations are tri-state: {@code null} (not set, inherited from the parent), {@code true}, or {@code false}
 * (explicitly cancels an inherited decoration). {@code bold()} is shorthand for {@code bold(true)}.
 */
public final class RichText {
    private static final int BOLD = 0;
    private static final int ITALIC = 1;
    private static final int UNDERLINED = 2;
    private static final int STRIKETHROUGH = 3;
    private static final int OBFUSCATED = 4;

    private final String text;
    private final String translationKey;
    private final List<RichText> arguments;
    private final List<RichText> children;
    private final Integer color;
    private final Boolean[] decorations;
    private final RichText hover;
    private final ClickAction click;

    private RichText(String text,
                     String translationKey,
                     List<RichText> arguments,
                     List<RichText> children,
                     Integer color,
                     Boolean[] decorations,
                     RichText hover,
                     ClickAction click) {
        this.text = text;
        this.translationKey = translationKey;
        this.arguments = arguments;
        this.children = children;
        this.color = color;
        this.decorations = decorations;
        this.hover = hover;
        this.click = click;
    }

    /**
     * A literal text. Legacy {@code §} codes in {@code content} are sent as they are and rendered by the client.
     */
    @NotNull
    public static RichText text(@NotNull String content) {
        return new RichText(Objects.requireNonNull(content),
                            null,
                            List.of(),
                            List.of(),
                            null,
                            new Boolean[5],
                            null,
                            null);
    }

    /**
     * A text the client translates with {@code key}, filling its placeholders with {@code args}.
     */
    @NotNull
    public static RichText translatable(@NotNull String key, @NotNull RichText... args) {
        return new RichText(null,
                            Objects.requireNonNull(key),
                            List.copyOf(Arrays.asList(args)),
                            List.of(),
                            null,
                            new Boolean[5],
                            null,
                            null);
    }

    /**
     * Sets the color as {@code 0xRRGGBB}. The 16 legacy chat colors are sent as named colors.
     */
    @NotNull
    public RichText color(int rgb) {
        return new RichText(text,
                            translationKey,
                            arguments,
                            children,
                            rgb & 0xFFFFFF,
                            decorations,
                            hover,
                            click);
    }

    @NotNull
    public RichText bold() {
        return bold(true);
    }

    @NotNull
    public RichText bold(boolean bold) {
        return decoration(BOLD, bold);
    }

    @NotNull
    public RichText italic() {
        return italic(true);
    }

    @NotNull
    public RichText italic(boolean italic) {
        return decoration(ITALIC, italic);
    }

    @NotNull
    public RichText underlined() {
        return underlined(true);
    }

    @NotNull
    public RichText underlined(boolean underlined) {
        return decoration(UNDERLINED, underlined);
    }

    @NotNull
    public RichText strikethrough() {
        return strikethrough(true);
    }

    @NotNull
    public RichText strikethrough(boolean strikethrough) {
        return decoration(STRIKETHROUGH, strikethrough);
    }

    @NotNull
    public RichText obfuscated() {
        return obfuscated(true);
    }

    @NotNull
    public RichText obfuscated(boolean obfuscated) {
        return decoration(OBFUSCATED, obfuscated);
    }

    private RichText decoration(int index, boolean state) {
        Boolean[] newDecorations = decorations.clone();
        newDecorations[index] = state;
        return new RichText(text, translationKey, arguments, children, color, newDecorations, hover, click);
    }

    /**
     * Sets the text shown while the pointer is over this text. {@code null}, or a text whose {@link #plainText()} is
     * empty, removes it, so an empty description never shows an empty tooltip.
     */
    @NotNull
    public RichText hover(@Nullable RichText hover) {
        RichText newHover = hover == null || hover.plainText()
                                                  .isEmpty() ? null : hover;
        return new RichText(text, translationKey, arguments, children, color, decorations, newHover, click);
    }

    /**
     * Sets what happens on click. {@code null} removes it.
     */
    @NotNull
    public RichText click(@Nullable ClickAction click) {
        return new RichText(text, translationKey, arguments, children, color, decorations, hover, click);
    }

    /**
     * Appends {@code children} after the current children.
     */
    @NotNull
    public RichText append(@NotNull RichText... children) {
        List<RichText> newChildren = new ArrayList<>(this.children);
        newChildren.addAll(Arrays.asList(children));
        return new RichText(text,
                            translationKey,
                            arguments,
                            List.copyOf(newChildren),
                            color,
                            decorations,
                            hover,
                            click);
    }

    /**
     * The literal text, or {@code null} for a translatable text.
     */
    @Nullable
    public String text() {
        return text;
    }

    /**
     * The translation key, or {@code null} for a literal text.
     */
    @Nullable
    public String translationKey() {
        return translationKey;
    }

    @NotNull
    public List<RichText> arguments() {
        return arguments;
    }

    @NotNull
    public List<RichText> children() {
        return children;
    }

    /**
     * The color as {@code 0xRRGGBB}, or {@code null} when inherited.
     */
    @Nullable
    public Integer color() {
        return color;
    }

    @Nullable
    public Boolean isBold() {
        return decorations[BOLD];
    }

    @Nullable
    public Boolean isItalic() {
        return decorations[ITALIC];
    }

    @Nullable
    public Boolean isUnderlined() {
        return decorations[UNDERLINED];
    }

    @Nullable
    public Boolean isStrikethrough() {
        return decorations[STRIKETHROUGH];
    }

    @Nullable
    public Boolean isObfuscated() {
        return decorations[OBFUSCATED];
    }

    @Nullable
    public RichText hover() {
        return hover;
    }

    @Nullable
    public ClickAction click() {
        return click;
    }

    /**
     * The texts of this text and its children joined. A translatable text contributes its key and not its arguments.
     */
    @NotNull
    public String plainText() {
        StringBuilder builder = new StringBuilder(text != null ? text : translationKey);
        children.forEach(child -> builder.append(child.plainText()));
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RichText)) {
            return false;
        }
        RichText that = (RichText) o;
        return Objects.equals(text, that.text)
                && Objects.equals(translationKey, that.translationKey)
                && arguments.equals(that.arguments)
                && children.equals(that.children)
                && Objects.equals(color, that.color)
                && Arrays.equals(decorations, that.decorations)
                && Objects.equals(hover, that.hover)
                && Objects.equals(click, that.click);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text,
                            translationKey,
                            arguments,
                            children,
                            color,
                            Arrays.hashCode(decorations),
                            hover,
                            click);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("RichText{");
        if (text != null) {
            builder.append("text=\"")
                   .append(text)
                   .append('"');
        } else {
            builder.append("translatable=\"")
                   .append(translationKey)
                   .append('"');
            if (!arguments.isEmpty()) {
                builder.append(", arguments=")
                       .append(arguments);
            }
        }
        if (color != null) {
            builder.append(String.format(", color=#%06x", color));
        }
        String[] names = {"bold", "italic", "underlined", "strikethrough", "obfuscated"};
        for (int i = 0; i < names.length; i++) {
            if (decorations[i] != null) {
                builder.append(", ")
                       .append(names[i])
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
        if (!children.isEmpty()) {
            builder.append(", children=")
                   .append(children);
        }
        return builder.append('}')
                      .toString();
    }
}
