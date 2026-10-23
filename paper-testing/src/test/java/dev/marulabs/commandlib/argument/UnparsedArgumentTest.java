package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnparsedArgumentTest {
    private CommandTester testerFor(String commandName) {
        return new CommandTester(() -> new Command(commandName) {{
            argument(new UnparsedArgument("text")).execute((ctx, text) -> ctx.sendMessage("raw:" + text));
        }}, "test.command");
    }

    @Test
    void player_selector_is_captured_as_raw_string() {
        FakeSender sender = FakeSender.player("Alice");
        try (CommandTester tester = testerFor("cmd")) {
            tester.execute("cmd @p", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("raw:@p");
    }

    @Test
    void slash_path_is_captured_as_single_token() {
        FakeSender sender = FakeSender.player("Alice");
        try (CommandTester tester = testerFor("cmd")) {
            tester.execute("cmd @../config/value", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("raw:@../config/value");
    }
}
