package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
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
