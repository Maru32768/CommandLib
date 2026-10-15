package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.NamespacedKey;

import java.util.Map;

public class NamespacedKeyArgument extends Argument<NamespacedKey, NamespacedKeyArgument> {
    public NamespacedKeyArgument(String name) {
        super(name,
              NMSArgumentNamespacedKey.create()
                                      .argument());
    }

    @Override
    public NamespacedKey cast(Object parsedArgument) {
        return (NamespacedKey) parsedArgument;
    }

    @Override
    protected NamespacedKey parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        String value = NMSArgumentNamespacedKey.create()
                                               .parse(ctx.getHandle(), name());
        NamespacedKey key = value.contains(":") ? NamespacedKey.fromString(value) : NamespacedKey.minecraft(value);
        if (key == null) {
            throw new ArgumentParseException(Caption.INVALID_KEY, Map.of("value", value));
        }
        return key;
    }
}
