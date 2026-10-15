package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatColorArgumentTest {
    @Test
    void color_is_read_by_its_name_and_formats_are_rejected() {
        List<String> messages;
        List<String> suggestions;

        try (CommandTester tester = new CommandTester(() -> new Command("color") {{
            argument(new ChatColorArgument("color")).execute((color, ctx) -> ctx.sendMessage(color.name()));
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("color dark_red", sender);
            tester.execute("color reset", sender);
            assertThatThrownBy(() -> tester.execute("color bold", sender)).isInstanceOf(RuntimeException.class);
            suggestions = tester.suggest("color dark_g", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("DARK_RED", "RESET");
        assertThat(suggestions).containsExactlyInAnyOrder("dark_gray", "dark_green");
    }
}
