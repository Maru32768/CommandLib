package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiomeArgumentTest {
    @Test
    void biome_is_resolved_by_registry_key() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("biome") {{
            argument(new BiomeArgument("biome")).execute((biome, ctx) -> ctx.sendMessage(biome.getKey()
                                                                                              .toString()));
        }}, "test.command")) {
            tester.execute("biome plains", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("minecraft:plains");
    }

    @Test
    void suggestions_include_biome_keys() throws Exception {
        try (CommandTester tester = new CommandTester(() -> new Command("biome") {{
            argument(new BiomeArgument("biome")).execute((biome, ctx) -> {
            });
        }}, "test.command")) {
            var suggestions = tester.suggestions("biome plai", FakeSender.player("Alice"))
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                   .contains("minecraft:plains");
        }
    }
}
