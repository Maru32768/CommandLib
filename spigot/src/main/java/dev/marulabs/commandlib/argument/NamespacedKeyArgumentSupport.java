package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.StringUtil;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentNamespacedKey;
import dev.marulabs.commandlib.suggestion.SuggestionBuilder;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

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

    static <T extends Keyed> T parseRegistryValue(CommandContext ctx,
                                                  String name,
                                                  Registry<T> registry,
                                                  Caption unknown) throws ArgumentParseException, CommandSyntaxException {
        String value = NMSArgumentNamespacedKey.create()
                                               .parse(ctx.getHandle(), name);
        T t = registry.get(parseKey(value));
        if (t == null) {
            throw new ArgumentParseException(unknown, Map.of("value", value));
        }
        return t;
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
