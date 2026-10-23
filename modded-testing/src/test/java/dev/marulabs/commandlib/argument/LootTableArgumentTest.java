package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

//? if >=1.20.5 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerRegistries;
*///?} elif >=1.20 {
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
//?} else
/*import net.minecraft.world.level.storage.loot.LootTables;*/

import static org.assertj.core.api.Assertions.assertThat;

class LootTableArgumentTest {
    @Test
    void loot_table_of_the_server_is_read_by_its_key_and_suggested() {
        List<String> messages;
        List<String> suggestions;
        ResourceLocation id = ResourceLocation.tryParse("minecraft:chests/simple_dungeon");

        try (CommandTester tester = new CommandTester(() -> new Command("loot") {{
            argument(new LootTableArgument("table")).execute((ctx, table) -> {
                ctx.sendMessage(String.valueOf(table == LootTable.EMPTY));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            //? if >=1.20.5 {
            /*ReloadableServerRegistries.Holder registries = Mockito.mock(ReloadableServerRegistries.Holder.class);
            Mockito.when(registries.getKeys(Registries.LOOT_TABLE))
                   .thenReturn(Set.of(id));
            Mockito.when(registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id)))
                   .thenReturn(LootTable.EMPTY);
            tester.withServer(server -> Mockito.when(server.reloadableRegistries())
                                               .thenReturn(registries));
            *///?} elif >=1.20 {
            LootDataManager lootData = Mockito.mock(LootDataManager.class);
            Mockito.when(lootData.getKeys(LootDataType.TABLE))
                   .thenReturn(Set.of(id));
            Mockito.when(lootData.getLootTable(id))
                   .thenReturn(LootTable.EMPTY);
            tester.withServer(server -> Mockito.when(server.getLootData())
                                               .thenReturn(lootData));
            //?} else {
            /*LootTables lootTables = Mockito.mock(LootTables.class);
            Mockito.when(lootTables.getIds())
                   .thenReturn(Set.of(id));
            Mockito.when(lootTables.get(id))
                   .thenReturn(LootTable.EMPTY);
            tester.withServer(server -> Mockito.when(server.getLootTables())
                                               .thenReturn(lootTables));
            *///?}

            tester.execute("loot minecraft:chests/simple_dungeon", sender);
            tester.execute("loot commandlib:missing", sender);
            suggestions = tester.suggest("loot dungeon", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("true", "commandlib:missing is not a known loot table");
        assertThat(suggestions).containsExactly("minecraft:chests/simple_dungeon");
    }
}
