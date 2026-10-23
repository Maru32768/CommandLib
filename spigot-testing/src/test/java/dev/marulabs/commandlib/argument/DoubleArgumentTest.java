package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DoubleArgumentTest {
    @Test
    void double_value_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("set") {{
            argument(new DoubleArgument("value")).execute((ctx, value) -> {
                ctx.sendMessage("value=" + value);
            });
        }}, "test.command")) {
            tester.execute("set 3.14", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("value=3.14");
    }
}
