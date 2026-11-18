package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntegerArgumentTest {
    @Test
    void integer_value_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("set") {{
            argument(new IntegerArgument("amount")).execute((ctx, amount) -> {
                ctx.sendMessage("amount=" + amount);
            });
        }}, "test.command")) {
            tester.execute("set 42", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("amount=42");
    }

    @Test
    void negative_integer_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("set") {{
            argument(new IntegerArgument("amount")).execute((ctx, amount) -> {
                ctx.sendMessage("amount=" + amount);
            });
        }}, "test.command")) {
            tester.execute("set -5", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("amount=-5");
    }
}
