package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeArgumentTest {
    // The argument is created inside the command supplier, where the tester stubs Paper's argument types.
    private static CommandTester tester(int minimum) {
        return new CommandTester(() -> new Command("wait") {{
            argument(new TimeArgument("ticks", minimum)).execute((ticks,
                                                                  ctx) -> ctx.sendMessage(String.valueOf(ticks)));
        }}, "test.command");
    }

    @Test
    void ticks_seconds_and_days_are_read_as_game_ticks() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = tester(0)) {
            tester.execute("wait 20", sender);
            tester.execute("wait 10s", sender);
            tester.execute("wait 0.5d", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("20", "200", "12000");
    }

    @Test
    void unknown_unit_and_too_few_ticks_are_syntax_errors() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = tester(20)) {
            assertThatThrownBy(() -> tester.execute("wait 1h",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
            assertThatThrownBy(() -> tester.execute("wait 10",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(sender.getSentMessageTexts()).isEmpty();
    }

    @Test
    void units_are_suggested_after_a_number() {
        try (CommandTester tester = tester(0)) {
            assertThat(tester.suggest("wait 5", FakeSender.player("Alice"))).containsExactlyInAnyOrder("d", "s", "t");
        }
    }
}
