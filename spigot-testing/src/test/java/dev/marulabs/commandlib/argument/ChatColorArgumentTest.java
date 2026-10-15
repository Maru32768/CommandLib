package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("DARK_RED", "RESET");
    }

    @Test
    void formats_are_rejected() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ChatColorArgumentTest::command, "test.command")) {
            tester.execute("color bold", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "color bold<--[HERE]");
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
