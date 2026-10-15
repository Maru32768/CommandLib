package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DurationArgumentTest {
    private static CommandTester tester() {
        return new CommandTester(() -> new Command("wait") {{
            argument(new DurationArgument("duration")).execute((duration,
                                                                ctx) -> ctx.sendMessage(duration.toString()));
        }}, "test.command");
    }

    @Test
    void duration_is_parsed_and_malformed_input_fails() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = tester()) {
            tester.execute("wait 1h30m", sender);
            tester.execute("wait 20t", sender);
            tester.execute("wait 5", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly(Duration.ofMinutes(90)
                                                                         .toString(),
                                                                 Duration.ofSeconds(1)
                                                                         .toString(),
                                                                 "command.unknown.argument",
                                                                 "wait 5" + "command.context.here");
    }

    @Test
    void a_number_is_suggested_with_each_unit() throws Exception {
        try (CommandTester tester = tester()) {
            assertThat(tester.suggestions("wait 2", FakeSender.player("Alice"))
                             .get()
                             .getList()).extracting(Suggestion::getText)
                                        .containsExactlyInAnyOrder("2d", "2h", "2m", "2s", "2t");
        }
    }
}
