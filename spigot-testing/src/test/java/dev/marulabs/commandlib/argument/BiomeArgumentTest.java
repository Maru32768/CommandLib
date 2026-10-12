package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiomeArgumentTest {
    @Test
    void biome_is_resolved_by_enum_name() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("biome") {{
            argument(new BiomeArgument("biome")).execute((biome, ctx) -> ctx.sendMessage(biome.name()));
        }}, "test.command")) {
            tester.execute("biome plains", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("PLAINS");
    }
}
