package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.internal.StringUtil;
import dev.marulabs.commandlib.suggestion.SuggestionBuilder;

import java.util.stream.Stream;

/**
 * Suggestions of an argument of type {@link StringArgumentType#string()}, which reads a word or a quoted string.
 */
final class QuotableSuggestions {
    private QuotableSuggestions() {
    }

    /**
     * Suggests each name that contains the input, ignoring case and an opening quote, quoted when it has a character a
     * word does not allow, so that the suggestion parses back to the name.
     */
    static void suggestMatching(SuggestionBuilder<?> sb, Stream<String> names) {
        String input = sb.getLatestInput();
        String typed = !input.isEmpty() && StringReader.isQuotedStringStart(input.charAt(0)) ? input.substring(1)
                : input;
        names.filter(x -> StringUtil.containsIgnoreCase(x, typed))
             .map(StringArgumentType::escapeIfRequired)
             .forEach(sb::suggest);
    }
}
