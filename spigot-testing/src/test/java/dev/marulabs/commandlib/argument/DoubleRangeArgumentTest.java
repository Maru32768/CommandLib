package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoubleRangeArgumentTest {
    private static Command rangeCommand() {
        return new Command("range") {{
            argument(new DoubleRangeArgument("range")).execute((range, ctx) -> ctx.sendMessage(range.toString()));
        }};
    }

    @Test
    void bounded_open_and_exact_ranges_are_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(DoubleRangeArgumentTest::rangeCommand, "test.command")) {
            tester.execute("range 0.5..10", sender);
            tester.execute("range ..5", sender);
            tester.execute("range 3.5..", sender);
            tester.execute("range 0.1", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly(Range.closed(0.5, 10.0)
                                                                      .toString(),
                                                                 Range.atMost(5.0)
                                                                      .toString(),
                                                                 Range.atLeast(3.5)
                                                                      .toString(),
                                                                 Range.singleton(0.1)
                                                                      .toString());
    }

    @Test
    void swapped_and_empty_ranges_are_syntax_errors() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(DoubleRangeArgumentTest::rangeCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("range 2.5..1",
                                                    sender)).hasMessageContaining("Min cannot be bigger than max");
            assertThatThrownBy(() -> tester.execute("range ..",
                                                    sender)).hasMessageContaining("Expected value or range of values");
        }

        assertThat(sender.getSentMessageTexts()).isEmpty();
    }
}
