package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs the vanilla int_range and float_range parsers of every node.
class RangeArgumentTest {
    @Test
    void vanilla_ranges_are_parsed_into_guava_ranges() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("range") {{
            argument(new IntegerRangeArgument("ints"),
                     new DoubleRangeArgument("doubles")).execute((ints, doubles, ctx) -> {
                         ctx.sendMessage(ints + " " + doubles);
                     });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("range 1..10 0.5..", sender);
            tester.execute("range ..5 0.1", sender);
            tester.execute("range 5 ..2.5", sender);
            assertThatThrownBy(() -> tester.execute("range 10..1 0.1",
                                                    sender)).hasMessageContaining("Min cannot be bigger than max");
            assertThatThrownBy(() -> tester.execute("range 1 ..",
                                                    sender)).hasMessageContaining("Expected value or range of values");
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly(Range.closed(1, 10) + " " + Range.atLeast(0.5),
                                             Range.atMost(5) + " " + Range.singleton(0.1),
                                             Range.singleton(5) + " " + Range.atMost(2.5));
    }
}
