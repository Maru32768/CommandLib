package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.NamespacedKey;

@SuppressWarnings("UnstableApiUsage")
public class NamespacedKeyArgument extends Argument<NamespacedKey, NamespacedKeyArgument> {
    public NamespacedKeyArgument(String name) {
        super(name, ArgumentTypes.namespacedKey());
    }

    @Override
    public NamespacedKey cast(Object parsedArgument) {
        return (NamespacedKey) parsedArgument;
    }

    @Override
    protected NamespacedKey parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), NamespacedKey.class);
    }
}
