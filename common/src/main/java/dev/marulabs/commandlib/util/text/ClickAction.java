package dev.marulabs.commandlib.util.text;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * What happens when a player clicks a {@link RichText}.
 */
public final class ClickAction {
    private final Type type;
    private final String value;

    private ClickAction(Type type, String value) {
        this.type = type;
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Runs {@code command} as the player. Include the leading slash for commands.
     */
    @NotNull
    public static ClickAction runCommand(@NotNull String command) {
        return new ClickAction(Type.RUN_COMMAND, command);
    }

    /**
     * Replaces the player's chat input with {@code command}.
     */
    @NotNull
    public static ClickAction suggestCommand(@NotNull String command) {
        return new ClickAction(Type.SUGGEST_COMMAND, command);
    }

    @NotNull
    public static ClickAction copyToClipboard(@NotNull String text) {
        return new ClickAction(Type.COPY_TO_CLIPBOARD, text);
    }

    @NotNull
    public static ClickAction openUrl(@NotNull String url) {
        return new ClickAction(Type.OPEN_URL, url);
    }

    @NotNull
    public Type type() {
        return type;
    }

    @NotNull
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClickAction)) {
            return false;
        }
        ClickAction that = (ClickAction) o;
        return type == that.type && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return type + "(" + value + ")";
    }

    public enum Type {
        RUN_COMMAND,
        SUGGEST_COMMAND,
        COPY_TO_CLIPBOARD,
        OPEN_URL
    }
}
