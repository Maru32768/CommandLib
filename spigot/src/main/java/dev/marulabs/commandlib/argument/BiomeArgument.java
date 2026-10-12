package dev.marulabs.commandlib.argument;

import org.bukkit.block.Biome;

public class BiomeArgument extends EnumArgument<Biome> {
    public BiomeArgument(String name) {
        super(name, Biome.class);
    }
}
