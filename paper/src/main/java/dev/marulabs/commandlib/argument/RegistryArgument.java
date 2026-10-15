package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;

/**
 * A value of a registry by its key, read by Paper's {@link ArgumentTypes#resource(RegistryKey)}, such as
 * {@code minecraft:sharpness} for {@code new RegistryArgument<>("enchantment", RegistryKey.ENCHANTMENT)}. Paper
 * suggests the registry's keys and rejects an unknown key while parsing.
 */
@SuppressWarnings("UnstableApiUsage")
public class RegistryArgument<T> extends Argument<T, RegistryArgument<T>> {
    public RegistryArgument(String name, RegistryKey<T> registryKey) {
        super(name, ArgumentTypes.resource(registryKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T cast(Object parsedArgument) {
        return (T) parsedArgument;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T parseImpl(CommandContext ctx) {
        return (T) ctx.getHandle()
                      .getArgument(name(), Object.class);
    }
}
