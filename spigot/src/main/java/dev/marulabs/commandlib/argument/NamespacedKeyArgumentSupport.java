package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.suggestion.SuggestionBuilder;
import dev.marulabs.commandlib.util.StringUtil;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Iterator;

final class NamespacedKeyArgumentSupport {
    private NamespacedKeyArgumentSupport() {
    }

    static NamespacedKey parseKey(String value) throws ArgumentParseException {
        NamespacedKey key = value.contains(":") ? NamespacedKey.fromString(value) : NamespacedKey.minecraft(value);
        if (key == null) {
            throw new ArgumentParseException(x -> x.sendFailure(value + " is not valid namespaced key"));
        }
        return key;
    }

    static <T extends Keyed> T parseRegistryValue(CommandContext ctx,
                                                  String name,
                                                  Registry<T> registry,
                                                  String description) throws ArgumentParseException, CommandSyntaxException {
        String value = NMSArgumentNamespacedKey.create()
                                               .parse(ctx.getHandle(), name);
        T t = registry.get(parseKey(value));
        if (t == null) {
            throw new ArgumentParseException(x -> x.sendFailure(value + " is not a known " + description));
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
