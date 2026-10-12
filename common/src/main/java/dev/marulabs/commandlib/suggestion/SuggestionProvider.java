package dev.marulabs.commandlib.suggestion;

import dev.marulabs.commandlib.CommonCommandContext;

import java.util.function.Consumer;

public interface SuggestionProvider<C extends CommonCommandContext<?, ?>> extends Consumer<SuggestionBuilder<C>> {
}
