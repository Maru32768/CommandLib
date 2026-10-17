package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegerRangeArgumentTest {
    private static Command rangeCommand() {
        return new Command("range") {{
            argument(new IntegerRangeArgument("range")).execute((range, ctx) -> ctx.sendMessage(range.toString()));
        }};
    }

    @Test
    void bounded_open_and_exact_ranges_are_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(IntegerRangeArgumentTest::rangeCommand, "test.command")) {
            tester.execute("range 1..10", sender);
            tester.execute("range ..5", sender);
            tester.execute("range 3..", sender);
            tester.execute("range 5", sender);
            tester.execute("range -3..-1", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly(Range.closed(1, 10)
                                                                      .toString(),
                                                                 Range.atMost(5)
                                                                      .toString(),
                                                                 Range.atLeast(3)
                                                                      .toString(),
                                                                 Range.singleton(5)
                                                                      .toString(),
                                                                 Range.closed(-3, -1)
                                                                      .toString());
    }

    @Test
    void swapped_empty_and_decimal_ranges_are_syntax_errors() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(IntegerRangeArgumentTest::rangeCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("range 10..1",
                                                    sender)).hasMessageContaining("Min cannot be bigger than max");
            assertThatThrownBy(() -> tester.execute("range ..",
                                                    sender)).hasMessageContaining("Expected value or range of values");
            assertThatThrownBy(() -> tester.execute("range 1.5", sender)).hasMessageContaining("Invalid number");
            assertThatThrownBy(() -> tester.execute("range +5", sender)).hasMessageContaining("Invalid number");
        }

        assertThat(sender.getSentMessageTexts()).isEmpty();
    }

    @Test
    void validator_receives_the_range() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("range") {{
            argument(new IntegerRangeArgument("range").validator(x -> x.hasUpperBound())).execute((range,
                                                                                                   ctx) -> ctx.sendMessage("ok"));
        }}, "test.command")) {
            tester.execute("range 1..2", sender);
            tester.execute("range 1..", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ok",
                                                                 "Incorrect argument for command",
                                                                 "range 1..<--[HERE]");
    }
}
