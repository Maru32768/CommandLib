package dev.marulabs.commandlib.suggestion;

import dev.marulabs.commandlib.CommonCommandContext;

import java.util.function.Consumer;

public interface SuggestionAction<C extends CommonCommandContext<?, ?>> extends Consumer<SuggestionBuilder<C>> {
}
