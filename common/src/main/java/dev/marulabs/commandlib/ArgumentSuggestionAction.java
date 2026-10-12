package dev.marulabs.commandlib;

import dev.marulabs.commandlib.suggestion.SuggestionAction;

import java.util.Objects;

final class ArgumentSuggestionAction<C extends CommonCommandContext<?, ?>> {
    private final SuggestionAction<C> action;

    private ArgumentSuggestionAction(SuggestionAction<C> action) {
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

    SuggestionAction<C> action() {
        return action;
    }
}
