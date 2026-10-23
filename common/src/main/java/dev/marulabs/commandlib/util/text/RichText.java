package dev.marulabs.commandlib.util.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A platform-independent, immutable chat message: a literal text ({@link TextRichText}) or a translation key
 * ({@link TranslatableRichText}), with a color, decorations, a hover text, a click action, and children.
 * <p>
 * Send it with {@code ctx.sendMessage(text)} on any platform. Every method that changes something returns a new
 * instance. Children inherit the style of their parent unless they set their own, as in Minecraft.
 * <p>
 * Decorations are tri-state: {@code null} (not set, inherited from the parent), {@code true}, or {@code false}
 * (explicitly cancels an inherited decoration). {@code bold()} is shorthand for {@code bold(true)}.
 */
public abstract class RichText {
    private final TextStyle style;
    private final List<RichText> children;

    // Package-private, so the kinds of text are the ones the platform converters know.
    RichText(TextStyle style, List<RichText> children) {
        this.style = style;
        this.children = children;
    }

    /**
     * A literal text. Legacy {@code §} codes in {@code content} are sent as they are and rendered by the client.
     */
    @NotNull
    public static TextRichText text(@NotNull String content) {
        return new TextRichText(Objects.requireNonNull(content), TextStyle.EMPTY, List.of());
    }

    /**
     * A text the client translates with {@code key}, filling its placeholders with {@code args}.
     */
    @NotNull
    public static TranslatableRichText translatable(@NotNull String key, @NotNull RichText... args) {
        return new TranslatableRichText(Objects.requireNonNull(key),
                                        List.copyOf(Arrays.asList(args)),
                                        TextStyle.EMPTY,
                                        List.of());
    }

    /**
     * The same text with another style and children.
     */
    abstract RichText with(TextStyle style, List<RichText> children);

    /**
     * The text of this node alone, without its children, for {@link #plainText()}.
     */
    abstract String plainContent();

    /**
     * Appends the {@link #toString()} part that tells what this node shows.
     */
    abstract void appendContent(StringBuilder builder);

    /**
     * Sets the color as {@code 0xRRGGBB}. The 16 legacy chat colors are sent as named colors.
     */
    @NotNull
    public RichText color(int rgb) {
        return with(style.color(rgb), children);
    }

    /**
     * Sets the color as {@link #color(int)} does unless this text has its own.
     */
    @NotNull
    public RichText colorIfAbsent(int rgb) {
        return style.color != null ? this : color(rgb);
    }

    @NotNull
    public RichText bold() {
        return bold(true);
    }

    @NotNull
    public RichText bold(boolean bold) {
        return with(style.decoration(TextStyle.BOLD, bold), children);
    }

    @NotNull
    public RichText italic() {
        return italic(true);
    }

    @NotNull
    public RichText italic(boolean italic) {
        return with(style.decoration(TextStyle.ITALIC, italic), children);
    }

    @NotNull
    public RichText underlined() {
        return underlined(true);
    }

    @NotNull
    public RichText underlined(boolean underlined) {
        return with(style.decoration(TextStyle.UNDERLINED, underlined), children);
    }

    @NotNull
    public RichText strikethrough() {
        return strikethrough(true);
    }

    @NotNull
    public RichText strikethrough(boolean strikethrough) {
        return with(style.decoration(TextStyle.STRIKETHROUGH, strikethrough), children);
    }

    @NotNull
    public RichText obfuscated() {
        return obfuscated(true);
    }

    @NotNull
    public RichText obfuscated(boolean obfuscated) {
        return with(style.decoration(TextStyle.OBFUSCATED, obfuscated), children);
    }

    /**
     * Sets the text shown while the pointer is over this text. {@code null}, or a text whose {@link #plainText()} is
     * empty, removes it, so an empty description never shows an empty tooltip.
     */
    @NotNull
    public RichText hover(@Nullable RichText hover) {
        RichText newHover = hover == null || hover.plainText()
                                                  .isEmpty() ? null : hover;
        return with(style.hover(newHover), children);
    }

    /**
     * Sets what happens on click. {@code null} removes it.
     */
    @NotNull
    public RichText click(@Nullable ClickAction click) {
        return with(style.click(click), children);
    }

    /**
     * Appends {@code children} after the current children.
     */
    @NotNull
    public RichText append(@NotNull RichText... children) {
        List<RichText> newChildren = new ArrayList<>(this.children);
        newChildren.addAll(Arrays.asList(children));
        return with(style, List.copyOf(newChildren));
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
        return style.color;
    }

    @Nullable
    public Boolean isBold() {
        return style.decoration(TextStyle.BOLD);
    }

    @Nullable
    public Boolean isItalic() {
        return style.decoration(TextStyle.ITALIC);
    }

    @Nullable
    public Boolean isUnderlined() {
        return style.decoration(TextStyle.UNDERLINED);
    }

    @Nullable
    public Boolean isStrikethrough() {
        return style.decoration(TextStyle.STRIKETHROUGH);
    }

    @Nullable
    public Boolean isObfuscated() {
        return style.decoration(TextStyle.OBFUSCATED);
    }

    @Nullable
    public RichText hover() {
        return style.hover;
    }

    @Nullable
    public ClickAction click() {
        return style.click;
    }

    /**
     * The texts of this text and its children joined. A translatable text contributes its key and not its arguments.
     */
    @NotNull
    public String plainText() {
        StringBuilder builder = new StringBuilder(plainContent());
        children.forEach(child -> builder.append(child.plainText()));
        return builder.toString();
    }

    /**
     * Compares the style and the children; each kind of text also compares what it shows.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RichText)) {
            return false;
        }
        RichText that = (RichText) o;
        return style.equals(that.style) && children.equals(that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(style, children);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("RichText{");
        appendContent(builder);
        style.appendTo(builder);
        if (!children.isEmpty()) {
            builder.append(", children=")
                   .append(children);
        }
        return builder.append('}')
                      .toString();
    }
}
