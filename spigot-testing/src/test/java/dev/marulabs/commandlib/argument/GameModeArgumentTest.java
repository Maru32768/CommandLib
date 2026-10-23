package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameModeArgumentTest {
    @Test
    void game_mode_is_parsed_case_insensitively() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("gamemode") {{
            argument(new GameModeArgument("mode")).execute((ctx, mode) -> ctx.sendMessage(mode.name()));
        }}, "test.command")) {
            tester.execute("gamemode creative", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("CREATIVE");
    }

    @Test
    void unknown_game_mode_sends_failure() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("gamemode") {{
            argument(new GameModeArgument("mode")).execute((ctx, mode) -> ctx.sendMessage(mode.name()));
        }}, "test.command")) {
            tester.execute("gamemode unknown", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "gamemode unknown<--[HERE]");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .getColor()
                         .getColor()).isEqualTo(new java.awt.Color(0xFF5555));
    }

    @Test
    void suggestions_include_all_game_modes() throws Exception {
        try (CommandTester tester = new CommandTester(new Command("gamemode") {{
            argument(new GameModeArgument("mode")).execute((ctx, mode) -> {
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Steve");

            var suggestions = tester.suggestions("gamemode ", sender)
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                   .contains("survival", "creative", "adventure", "spectator");
        }
    }
}
