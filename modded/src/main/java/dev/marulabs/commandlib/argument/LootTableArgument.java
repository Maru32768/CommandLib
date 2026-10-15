package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;
//? if >=1.20.5 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?} elif >=1.20
import net.minecraft.world.level.storage.loot.LootDataType;

import java.util.Collection;
import java.util.Map;

/**
 * A loot table of the server by its key, such as {@code minecraft:chests/simple_dungeon}. Input without a namespace is
 * in {@code minecraft}, and the loaded loot tables are suggested. An unknown key fails when the command runs.
 */
public class LootTableArgument extends Argument<LootTable, LootTableArgument> {
    public LootTableArgument(String name) {
        super(name, net.minecraft.commands.arguments.ResourceLocationArgument.id());
        addSuggestionProvider(sb -> RegistryEntries.suggest(sb,
                                                            keys(RegistryEntries.server(sb.getContext())).stream()));
    }

    @Override
    public LootTable cast(Object parsedArgument) {
        return (LootTable) parsedArgument;
    }

    @Override
    protected LootTable parseImpl(CommandContext ctx) throws ArgumentParseException {
        ResourceLocation key = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx.getHandle(), name());
        MinecraftServer server = RegistryEntries.server(ctx);
        // The lookup returns an empty table for an unknown key.
        if (!keys(server).contains(key)) {
            throw new ArgumentParseException(Caption.UNKNOWN_LOOT_TABLE, Map.of("value", key));
        }
        //? if >=1.20.5 {
        /*return server.reloadableRegistries()
                     .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, key));
        *///?} elif >=1.20 {
        return server.getLootData()
                     .getLootTable(key);
        //?} else {
        /*return server.getLootTables()
                     .get(key);
        *///?}
    }

    private static Collection<ResourceLocation> keys(MinecraftServer server) {
        //? if >=1.20.5 {
        /*return server.reloadableRegistries()
                     .getKeys(Registries.LOOT_TABLE);
        *///?} elif >=1.20 {
        return server.getLootData()
                     .getKeys(LootDataType.TABLE);
        //?} else {
        /*return server.getLootTables()
                     .getIds();
        *///?}
    }
}
