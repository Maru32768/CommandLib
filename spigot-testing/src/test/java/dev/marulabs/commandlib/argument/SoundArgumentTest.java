package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoundArgumentTest {
    @Test
    void sound_is_resolved_by_registry_key() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("sound") {{
            argument(new SoundArgument("value")).execute((ctx, sound) -> ctx.sendMessage(sound.getKey()
                                                                                              .toString()));
        }}, "test.command")) {
            tester.execute("sound minecraft:block.anvil.land", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("minecraft:block.anvil.land");
    }

    @Test
    void unknown_sound_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("sound") {{
            argument(new SoundArgument("value")).execute((ctx, sound) -> ctx.sendMessage("unreachable"));
        }}, "test.command")) {
            tester.execute("sound commandlib:missing", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("commandlib:missing is not a known sound");
    }
}
