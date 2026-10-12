package dev.marulabs.commandlib;

import dev.marulabs.commandlib.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class ArgumentSuggestionProvider<C extends CommonCommandContext<?, ?>> {
    // Null for the argument type's own suggestions, which need the Brigadier context instead of a SuggestionBuilder.
    private final @Nullable SuggestionProvider<C> action;

    private ArgumentSuggestionProvider(@Nullable SuggestionProvider<C> action) {
        this.action = action;
    }

    static <C extends CommonCommandContext<?, ?>> ArgumentSuggestionProvider<C> defaults() {
        return new ArgumentSuggestionProvider<>(null);
    }

    static <C extends CommonCommandContext<?, ?>> ArgumentSuggestionProvider<C> custom(SuggestionProvider<C> action) {
        return new ArgumentSuggestionProvider<>(Objects.requireNonNull(action));
    }

    boolean isDefaultSuggestions() {
        return action == null;
    }

    @Nullable
    SuggestionProvider<C> action() {
        return action;
    }
}
