package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
//? if >=1.19.3 {
import net.minecraft.core.registries.Registries;
//?} else
/*import net.minecraft.core.Registry;*/
import net.minecraft.world.level.biome.Biome;

/**
 * A biome of the server's biome registry, which data packs fill, by its key such as {@code minecraft:plains}. Input
 * without a namespace is in {@code minecraft}, and the registry's keys are suggested. An unknown key fails when the
 * command runs.
 */
public class BiomeArgument extends Argument<Biome, BiomeArgument> {
    public BiomeArgument(String name) {
        super(name, net.minecraft.commands.arguments.ResourceLocationArgument.id());
        //? if >=1.19.3 {
        addSuggestionProvider(sb -> RegistryEntries.suggestKeys(sb, Registries.BIOME));
        //?} else
        /*addSuggestionProvider(sb -> RegistryEntries.suggestKeys(sb, Registry.BIOME_REGISTRY));*/
    }

    @Override
    public Biome cast(Object parsedArgument) {
        return (Biome) parsedArgument;
    }

    @Override
    protected Biome parseImpl(CommandContext ctx) throws ArgumentParseException {
        //? if >=1.19.3 {
        return RegistryEntries.get(ctx, name(), Registries.BIOME, Caption.UNKNOWN_BIOME);
        //?} else
        /*return RegistryEntries.get(ctx, name(), Registry.BIOME_REGISTRY, Caption.UNKNOWN_BIOME);*/
    }
}
