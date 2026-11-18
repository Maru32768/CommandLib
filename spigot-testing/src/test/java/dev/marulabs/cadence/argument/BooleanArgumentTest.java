package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BooleanArgumentTest {
    @Test
    void true_value_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("toggle") {{
            argument(new BooleanArgument("value")).execute((ctx, value) -> {
                ctx.sendMessage(value ? "on" : "off");
            });
        }}, "test.command")) {
            tester.execute("toggle true", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("on");
    }

    @Test
    void false_value_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("toggle") {{
            argument(new BooleanArgument("value")).execute((ctx, value) -> {
                ctx.sendMessage(value ? "on" : "off");
            });
        }}, "test.command")) {
            tester.execute("toggle false", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("off");
    }
}
