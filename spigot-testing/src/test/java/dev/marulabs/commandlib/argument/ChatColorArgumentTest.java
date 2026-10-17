package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatColorArgumentTest {
    private static Command command() {
        return new Command("color") {{
            argument(new ChatColorArgument("value")).execute((color, ctx) -> ctx.sendMessage(color.name()));
        }};
    }

    @Test
    void colors_and_reset_are_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ChatColorArgumentTest::command, "test.command")) {
            tester.execute("color dark_red", sender);
            tester.execute("color reset", sender);
            // Vanilla ignores case and characters other than letters.
            tester.execute("color DARK_RED", sender);
            tester.execute("color darkred", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("DARK_RED", "RESET", "DARK_RED", "DARK_RED");
    }

    @Test
    void formats_are_syntax_errors() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ChatColorArgumentTest::command, "test.command")) {
            assertThatThrownBy(() -> tester.execute("color bold", sender)).hasMessageContaining("Invalid color 'bold'");
        }

        assertThat(sender.getSentMessageTexts()).isEmpty();
    }

    @Test
    void suggestions_are_colors() throws Exception {
        try (CommandTester tester = new CommandTester(ChatColorArgumentTest::command, "test.command")) {
            var suggestions = tester.suggestions("color ", FakeSender.player("Alice"))
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(Suggestion::getText)
                                   .contains("dark_red", "reset")
                                   .doesNotContain("bold", "magic");
        }
    }
}
