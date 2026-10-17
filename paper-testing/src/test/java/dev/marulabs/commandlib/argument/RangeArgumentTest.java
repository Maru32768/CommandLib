package dev.marulabs.commandlib.argument;

import com.google.common.collect.Range;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangeArgumentTest {
    private static CommandTester tester() {
        return CommandTester.builder()
                            .command(() -> new Command("ints") {{
                                argument(new IntegerRangeArgument("range")).execute((range,
                                                                                     ctx) -> ctx.sendMessage(range.toString()));
                            }})
                            .command(() -> new Command("doubles") {{
                                argument(new DoubleRangeArgument("range")).execute((range,
                                                                                    ctx) -> ctx.sendMessage(range.toString()));
                            }})
                            .permissionPrefix("test.command")
                            .build();
    }

    @Test
    void ranges_are_parsed_into_guava_ranges() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = tester()) {
            tester.execute("ints 1..10", sender);
            tester.execute("ints ..5", sender);
            tester.execute("ints 5", sender);
            tester.execute("doubles 3.5..", sender);
            tester.execute("doubles 0.1", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly(Range.closed(1, 10)
                                                                      .toString(),
                                                                 Range.atMost(5)
                                                                      .toString(),
                                                                 Range.singleton(5)
                                                                      .toString(),
                                                                 Range.atLeast(3.5)
                                                                      .toString(),
                                                                 Range.singleton(0.1)
                                                                      .toString());
    }

    @Test
    void swapped_and_empty_ranges_are_syntax_errors() {
        try (CommandTester tester = tester()) {
            FakeSender sender = FakeSender.player("Alice");
            assertThatThrownBy(() -> tester.execute("ints 10..1",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
            assertThatThrownBy(() -> tester.execute("doubles ..",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
            // Java reads these as numbers, vanilla does not.
            List.of("doubles 1e3", "doubles NaN", "doubles +5", "ints +5")
                .forEach(input -> assertThatThrownBy(() -> tester.execute(input, sender)).as(input)
                                                                                         .hasMessageContaining("Invalid number"));
        }
    }
}
