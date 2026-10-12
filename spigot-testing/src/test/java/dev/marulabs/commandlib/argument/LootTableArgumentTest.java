package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.loot.LootTable;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class LootTableArgumentTest {
    @Test
    void loot_table_is_resolved_by_namespaced_key() {
        FakeSender sender = FakeSender.player("Alice");
        NamespacedKey key = NamespacedKey.minecraft("chests/simple_dungeon");
        LootTable lootTable = Mockito.mock(LootTable.class);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getLootTable(key))
                  .thenReturn(lootTable);
            try (CommandTester tester = new CommandTester(() -> new Command("loot") {{
                argument(new LootTableArgument("value")).execute((value, ctx) -> ctx.sendMessage("ok"));
            }}, "test.command")) {
                tester.execute("loot chests/simple_dungeon", sender);
            }
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ok");
    }

    @Test
    void suggestions_include_loot_table_keys() throws Exception {
        try (CommandTester tester = new CommandTester(() -> new Command("loot") {{
            argument(new LootTableArgument("value")).execute((value, ctx) -> {
            });
        }}, "test.command")) {
            var suggestions = tester.suggestions("loot simple", FakeSender.player("Alice"))
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                   .contains("minecraft:chests/simple_dungeon");
        }
    }
}
