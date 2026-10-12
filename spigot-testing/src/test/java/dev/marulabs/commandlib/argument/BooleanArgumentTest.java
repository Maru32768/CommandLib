package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BooleanArgumentTest {
    @Test
    void true_value_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("toggle") {{
            argument(new BooleanArgument("value")).execute((value, ctx) -> {
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
            argument(new BooleanArgument("value")).execute((value, ctx) -> {
                ctx.sendMessage(value ? "on" : "off");
            });
        }}, "test.command")) {
            tester.execute("toggle false", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("off");
    }
}
