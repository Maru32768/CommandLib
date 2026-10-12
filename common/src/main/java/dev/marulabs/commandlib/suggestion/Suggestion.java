package dev.marulabs.commandlib.suggestion;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Suggestion {
    private final String text;
    private final @Nullable Message tooltipMessage;

    public Suggestion(@NotNull String text, @Nullable String tooltip) {
        this.text = Objects.requireNonNull(text);
        this.tooltipMessage = tooltip != null ? new LiteralMessage(tooltip) : null;
    }

    public Suggestion(@NotNull String text, @NotNull Message tooltipMessage) {
        this.text = Objects.requireNonNull(text);
        this.tooltipMessage = Objects.requireNonNull(tooltipMessage);
    }

    public void suggest(SuggestionsBuilder sb) {
        if (tooltipMessage != null) {
            sb.suggest(text, tooltipMessage);
        } else {
            sb.suggest(text);
        }
    }
}
