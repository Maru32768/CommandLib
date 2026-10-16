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

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LootTableArgumentTest {
    private static Command command(AtomicReference<LootTable> received) {
        return new Command("loot") {{
            argument(new LootTableArgument("value")).execute((value, ctx) -> received.set(value));
        }};
    }

    @Test
    void loot_table_is_resolved_by_namespaced_key() {
        AtomicReference<LootTable> received = new AtomicReference<>();
        LootTable lootTable = Mockito.mock(LootTable.class);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> command(received), "test.command")) {
            bukkit.when(() -> Bukkit.getLootTable(NamespacedKey.minecraft("chests/simple_dungeon")))
                  .thenReturn(lootTable);
            tester.execute("loot chests/simple_dungeon", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(lootTable);
    }

    @Test
    void unknown_loot_table_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");
        AtomicReference<LootTable> received = new AtomicReference<>();

        try (MockedStatic<Bukkit> ignored = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> command(received), "test.command")) {
            tester.execute("loot commandlib:missing", sender);
        }

        assertThat(received).hasValue(null);
        assertThat(sender.getSentMessageTexts()).containsExactly("commandlib:missing is not a known loot table");
    }

    @Test
    void suggestions_are_loot_table_keys_containing_the_input() {
        try (CommandTester tester = new CommandTester(() -> command(new AtomicReference<>()), "test.command")) {
            assertThat(tester.suggest("loot SIMPLE_DUNGEON",
                                      FakeSender.player("Alice"))).containsExactly("minecraft:chests/simple_dungeon");
        }
    }
}
