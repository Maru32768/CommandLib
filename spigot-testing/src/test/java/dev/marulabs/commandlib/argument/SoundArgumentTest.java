package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoundArgumentTest {
    @Test
    void sound_is_parsed_case_insensitively() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("sound") {{
            argument(new SoundArgument("value")).execute((sound, ctx) -> ctx.sendMessage(sound.name()));
        }}, "test.command")) {
            tester.execute("sound block_anvil_land", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("BLOCK_ANVIL_LAND");
    }
}
