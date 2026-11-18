package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiomeArgumentTest {
    @Test
    void biome_is_resolved_by_registry_key() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("biome") {{
            argument(new BiomeArgument("biome")).execute((ctx, biome) -> ctx.sendMessage(biome.getKey()
                                                                                              .toString()));
        }}, "test.command")) {
            tester.execute("biome plains", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("minecraft:plains");
    }

    @Test
    void suggestions_include_biome_keys() throws Exception {
        try (CommandTester tester = new CommandTester(() -> new Command("biome") {{
            argument(new BiomeArgument("biome")).execute((ctx, biome) -> {
            });
        }}, "test.command")) {
            assertThat(tester.suggest("biome plai", FakeSender.player("Alice"))).contains("minecraft:plains");
        }
    }
}
