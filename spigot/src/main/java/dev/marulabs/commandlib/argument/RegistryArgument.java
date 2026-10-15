package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
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
        return NamespacedKeyArgumentSupport.parseRegistryValue(ctx, name(), registry, "registry entry");
    }
}
