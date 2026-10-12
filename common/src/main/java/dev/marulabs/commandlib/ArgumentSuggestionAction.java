package dev.marulabs.commandlib;

import dev.marulabs.commandlib.suggestion.SuggestionAction;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class ArgumentSuggestionAction<C extends CommonCommandContext<?, ?>> {
    // Null for the argument type's own suggestions, which need the Brigadier context instead of a SuggestionBuilder.
    private final @Nullable SuggestionAction<C> action;

    private ArgumentSuggestionAction(@Nullable SuggestionAction<C> action) {
        this.action = action;
    }

    static <C extends CommonCommandContext<?, ?>> ArgumentSuggestionAction<C> defaults() {
        return new ArgumentSuggestionAction<>(null);
    }

    static <C extends CommonCommandContext<?, ?>> ArgumentSuggestionAction<C> custom(SuggestionAction<C> action) {
        return new ArgumentSuggestionAction<>(Objects.requireNonNull(action));
    }

    boolean isDefaultSuggestions() {
        return action == null;
    }

    @Nullable
    SuggestionAction<C> action() {
        return action;
    }
}
