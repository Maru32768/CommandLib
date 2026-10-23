package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DurationArgumentTest {
    @Test
    void duration_is_parsed_and_malformed_input_fails() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("wait") {{
            argument(new DurationArgument("duration")).execute((ctx, duration) -> {
                ctx.sendMessage(duration.toString());
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("wait 1h30m", sender);
            tester.execute("wait 1m1m", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly(Duration.ofMinutes(90)
                                                     .toString(),
                                             "Incorrect argument for command",
                                             "§7wait §c§n1m1m§r<--[HERE]");
    }
}
