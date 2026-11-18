package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.Caption;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.Keyed;
import org.bukkit.Registry;

/**
 * A value of a Bukkit {@link Registry} by its key, such as {@code minecraft:sharpness} for
 * {@code new RegistryArgument<>("enchantment", Registry.ENCHANTMENT)}. Input without a namespace is in
 * {@code minecraft}, and the registry's keys are suggested. An unknown key fails when the command runs.
 */
public class RegistryArgument<T extends Keyed> extends Argument<T, RegistryArgument<T>> {
    private final Registry<T> registry;

    public RegistryArgument(String name, Registry<T> registry) {
        super(name,
              NMSArgumentNamespacedKey.create()
                                      .argument());
        this.registry = registry;
        addSuggestionProvider(sb -> NamespacedKeyArgumentSupport.suggestKeys(sb, registry));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T cast(Object parsedArgument) {
        return (T) parsedArgument;
    }

    @Override
    protected T parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NamespacedKeyArgumentSupport.parseRegistryValue(ctx, name(), registry, Caption.UNKNOWN_REGISTRY_ENTRY);
    }
}
