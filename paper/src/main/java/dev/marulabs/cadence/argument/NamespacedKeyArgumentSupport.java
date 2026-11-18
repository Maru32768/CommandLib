package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Caption;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.StringUtil;
import dev.marulabs.cadence.suggestion.SuggestionBuilder;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;

import java.util.Iterator;
import java.util.Map;

final class NamespacedKeyArgumentSupport {
    private NamespacedKeyArgumentSupport() {
    }

    static NamespacedKey parseKey(String value) throws ArgumentParseException {
        NamespacedKey key = value.contains(":") ? NamespacedKey.fromString(value) : NamespacedKey.minecraft(value);
        if (key == null) {
            throw new ArgumentParseException(Caption.INVALID_KEY, Map.of("value", value));
        }
        return key;
    }

    static void suggestKeys(SuggestionBuilder<?> builder, Iterator<? extends Keyed> keyedValues) {
        while (keyedValues.hasNext()) {
            suggestKey(builder,
                       keyedValues.next()
                                  .getKey());
        }
    }

    static void suggestKeys(SuggestionBuilder<?> builder, Iterable<? extends Keyed> keyedValues) {
        for (Keyed keyed : keyedValues) {
            suggestKey(builder, keyed.getKey());
        }
    }

    static void suggestKey(SuggestionBuilder<?> builder, NamespacedKey key) {
        String value = key.toString();
        if (builder.getLatestInput()
                   .isEmpty()
                || StringUtil.containsIgnoreCase(value, builder.getLatestInput())) {
            builder.suggest(value);
        }
    }
}
