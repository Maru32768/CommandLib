package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DurationArgumentTest {
    private static Command waitCommand() {
        return new Command("wait") {{
            argument(new DurationArgument("duration")).execute((ctx,
                                                                duration) -> ctx.sendMessage(String.valueOf(duration.toMillis())));
        }};
    }

    @Test
    void units_are_parsed_and_added_up() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(DurationArgumentTest::waitCommand, "test.command")) {
            tester.execute("wait 20t", sender);
            tester.execute("wait 5s", sender);
            tester.execute("wait 1h30m", sender);
            tester.execute("wait 2d", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("1000",
                                                                 "5000",
                                                                 String.valueOf(Duration.ofMinutes(90)
                                                                                        .toMillis()),
                                                                 String.valueOf(Duration.ofDays(2)
                                                                                        .toMillis()));
    }

    @Test
    void malformed_durations_send_failure_messages() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(DurationArgumentTest::waitCommand, "test.command")) {
            tester.execute("wait 5", sender);
            tester.execute("wait 5w", sender);
            tester.execute("wait 30s1h", sender);
            tester.execute("wait 99999999999999999999d", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "wait 5<--[HERE]",
                                                                 "Incorrect argument for command",
                                                                 "wait 5w<--[HERE]",
                                                                 "Incorrect argument for command",
                                                                 "wait 30s1h<--[HERE]",
                                                                 "Incorrect argument for command",
                                                                 "wait 99999999999999999999d<--[HERE]");
    }

    @Test
    void validator_can_bound_the_duration() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("wait") {{
            argument(new DurationArgument("duration").validator(x -> x.compareTo(Duration.ofHours(1)) <= 0)).execute((ctx,
                                                                                                                      duration) -> ctx.sendMessage("ok"));
        }}, "test.command")) {
            tester.execute("wait 59m", sender);
            tester.execute("wait 2h", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ok",
                                                                 "Incorrect argument for command",
                                                                 "wait 2h<--[HERE]");
    }

    @Test
    void a_number_is_suggested_with_each_unit() throws Exception {
        try (CommandTester tester = new CommandTester(DurationArgumentTest::waitCommand, "test.command")) {
            assertThat(tester.suggestions("wait 1h3", FakeSender.player("Alice"))
                             .get()
                             .getList()).extracting(Suggestion::getText)
                                        .containsExactlyInAnyOrder("1h3m", "1h3s", "1h3t");
            assertThat(tester.suggestions("wait 1h", FakeSender.player("Alice"))
                             .get()
                             .getList()).isEmpty();
            assertThat(tester.suggestions("wait ", FakeSender.player("Alice"))
                             .get()
                             .getList()).extracting(Suggestion::getText)
                                        .noneMatch(x -> x.matches("\\d.*"));
        }
    }
}
