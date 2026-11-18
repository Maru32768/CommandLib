package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.Caption;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.loot.LootTable;

import java.util.Map;

public class LootTableArgument extends Argument<LootTable, LootTableArgument> {
    public LootTableArgument(String name) {
        super(name,
              NMSArgumentNamespacedKey.create()
                                      .argument());
        addSuggestionProvider(sb -> NamespacedKeyArgumentSupport.suggestKeys(sb, Registry.LOOT_TABLES));
    }

    @Override
    public LootTable cast(Object parsedArgument) {
        return (LootTable) parsedArgument;
    }

    @Override
    protected LootTable parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        String value = NMSArgumentNamespacedKey.create()
                                               .parse(ctx.getHandle(), name());
        NamespacedKey key = NamespacedKeyArgumentSupport.parseKey(value);
        LootTable lootTable = Bukkit.getLootTable(key);
        if (lootTable == null) {
            throw new ArgumentParseException(Caption.UNKNOWN_LOOT_TABLE, Map.of("value", value));
        }
        return lootTable;
    }
}
