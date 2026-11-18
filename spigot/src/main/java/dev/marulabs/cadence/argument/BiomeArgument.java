package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.Caption;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;

public class BiomeArgument extends Argument<Biome, BiomeArgument> {
    public BiomeArgument(String name) {
        super(name,
              NMSArgumentNamespacedKey.create()
                                      .argument());
        addSuggestionProvider(sb -> NamespacedKeyArgumentSupport.suggestKeys(sb, Registry.BIOME));
    }

    @Override
    public Biome cast(Object parsedArgument) {
        return (Biome) parsedArgument;
    }

    @Override
    protected Biome parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NamespacedKeyArgumentSupport.parseRegistryValue(ctx, name(), Registry.BIOME, Caption.UNKNOWN_BIOME);
    }
}
