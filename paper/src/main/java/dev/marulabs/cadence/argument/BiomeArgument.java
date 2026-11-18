package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.block.Biome;

@SuppressWarnings("UnstableApiUsage")
public class BiomeArgument extends Argument<Biome, BiomeArgument> {
    public BiomeArgument(String name) {
        super(name, ArgumentTypes.resource(RegistryKey.BIOME));
    }

    @Override
    public Biome cast(Object parsedArgument) {
        return (Biome) parsedArgument;
    }

    @Override
    protected Biome parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), Biome.class);
    }
}
