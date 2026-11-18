package dev.marulabs.cadence.suggestion;

import dev.marulabs.cadence.CommonCommandContext;

import java.util.function.Consumer;

public interface SuggestionProvider<C extends CommonCommandContext<?, ?>> extends Consumer<SuggestionBuilder<C>> {
}
