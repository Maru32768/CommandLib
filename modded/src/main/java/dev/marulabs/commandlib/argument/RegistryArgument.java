package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * A value of a registry of the server by its key, such as {@code minecraft:diamond} for
 * {@code new RegistryArgument<>("item", Registries.ITEM)} ({@code Registry.ITEM_REGISTRY} before 1.19.3). Input without a
 * namespace is in {@code minecraft}, and the registry's keys are suggested. The registry is looked up in the server's
 * registries, so data-driven ones such as biomes work. An unknown key fails when the command runs.
 */
public class RegistryArgument<T> extends Argument<T, RegistryArgument<T>> {
    private final ResourceKey<? extends Registry<T>> registryKey;

    public RegistryArgument(String name, ResourceKey<? extends Registry<T>> registryKey) {
        super(name, net.minecraft.commands.arguments.ResourceLocationArgument.id());
        this.registryKey = registryKey;
        addSuggestionProvider(sb -> RegistryEntries.suggestKeys(sb, registryKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T cast(Object parsedArgument) {
        return (T) parsedArgument;
    }

    @Override
    protected T parseImpl(CommandContext ctx) throws ArgumentParseException {
        return RegistryEntries.get(ctx, name(), registryKey, Caption.UNKNOWN_REGISTRY_ENTRY);
    }
}
