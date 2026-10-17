package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import dev.marulabs.commandlib.nms.argument.MockNMSArgumentTime;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeArgumentTest {
    private static Command waitCommand(TimeArgument argument) {
        return new Command("wait") {{
            argument(argument).execute((ticks, ctx) -> ctx.sendMessage(String.valueOf(ticks)));
        }};
    }

    @Test
    void ticks_seconds_and_days_are_read_as_game_ticks() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> waitCommand(new TimeArgument("ticks")), "test.command")) {
            tester.execute("wait 20", sender);
            tester.execute("wait 3t", sender);
            tester.execute("wait 10s", sender);
            tester.execute("wait 0.5d", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("20", "3", "200", "12000");
    }

    @Test
    void unknown_unit_and_too_few_ticks_are_syntax_errors() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> waitCommand(new TimeArgument("ticks", 20)),
                                                      "test.command")) {
            assertThatThrownBy(() -> tester.execute("wait 1h", sender)).hasMessageContaining("Invalid unit");
            assertThatThrownBy(() -> tester.execute("wait 10",
                                                    sender)).hasMessageContaining("Tick count must not be less than 20, found 10");
            tester.execute("wait 1s", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("20");
    }

    // The type of servers before 1.19.4, which takes no minimum.
    public static class MockNMSArgumentTimeWithoutMinimum extends MockNMSArgumentTime {
        @Override
        public ArgumentType<?> argument(int minimum) {
            return super.argument(0);
        }
    }

    @Test
    void too_few_ticks_fail_where_the_type_has_no_minimum() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = CommandTester.builder()
                                                 .mockNmsClass(NMSArgumentTime.class,
                                                               MockNMSArgumentTimeWithoutMinimum.class)
                                                 .command(() -> waitCommand(new TimeArgument("ticks", 20)))
                                                 .permissionPrefix("test.command")
                                                 .build()) {
            tester.execute("wait 10", sender);
            tester.execute("wait 1s", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Tick count must not be less than 20, found 10", "20");
    }

    @Test
    void units_are_suggested_after_a_number() throws Exception {
        try (CommandTester tester = new CommandTester(() -> waitCommand(new TimeArgument("ticks")), "test.command")) {
            var suggestions = tester.suggestions("wait 5", FakeSender.player("Alice"))
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(Suggestion::getText)
                                   .containsExactlyInAnyOrder("d", "s", "t");
        }
    }
}
