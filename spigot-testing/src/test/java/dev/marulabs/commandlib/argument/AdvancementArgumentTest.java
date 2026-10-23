package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancementArgumentTest {
    @Test
    void advancement_is_resolved_by_namespaced_key() {
        FakeSender sender = FakeSender.player("Alice");
        NamespacedKey key = NamespacedKey.minecraft("story/mine_stone");
        Advancement advancement = Mockito.mock(Advancement.class);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getAdvancement(key))
                  .thenReturn(advancement);
            try (CommandTester tester = new CommandTester(() -> new Command("advancement") {{
                argument(new AdvancementArgument("value")).execute((ctx, value) -> ctx.sendMessage("ok"));
            }}, "test.command")) {
                tester.execute("advancement story/mine_stone", sender);
            }
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ok");
    }

    @Test
    void suggestions_include_advancement_keys() throws Exception {
        NamespacedKey key = NamespacedKey.minecraft("story/mine_stone");
        Advancement advancement = Mockito.mock(Advancement.class);
        Mockito.when(advancement.getKey())
               .thenReturn(key);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::advancementIterator)
                  .thenReturn(java.util.List.of(advancement)
                                            .iterator());
            try (CommandTester tester = new CommandTester(() -> new Command("advancement") {{
                argument(new AdvancementArgument("value")).execute((ctx, value) -> {
                });
            }}, "test.command")) {
                var suggestions = tester.suggestions("advancement story", FakeSender.player("Alice"))
                                        .get()
                                        .getList();

                assertThat(suggestions).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                       .contains("minecraft:story/mine_stone");
            }
        }
    }
}
