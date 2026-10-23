package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.sounds.SoundEvents;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SoundArgumentTest {
    @Test
    void sound_event_is_read_by_its_key_and_suggested() {
        List<String> messages;
        List<String> suggestions;

        try (CommandTester tester = new CommandTester(() -> new Command("sound") {{
            argument(new SoundArgument("sound")).execute((ctx, sound) -> {
                ctx.sendMessage(String.valueOf(sound == SoundEvents.PIG_AMBIENT));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("sound minecraft:entity.pig.ambient", sender);
            tester.execute("sound entity.pig.ambient", sender);
            tester.execute("sound commandlib:missing", sender);
            suggestions = tester.suggest("sound pig.amb", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("true", "true", "commandlib:missing is not a known sound");
        assertThat(suggestions).containsExactly("minecraft:entity.pig.ambient");
    }
}
