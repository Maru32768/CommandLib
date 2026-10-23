package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameModeArgumentTest {
    private static CommandTester tester() {
        return new CommandTester(() -> new Command("mode") {{
            argument(new GameModeArgument("mode")).execute((ctx, mode) -> ctx.sendMessage(mode.name()));
        }}, "test.command");
    }

    @Test
    void game_mode_is_read_by_its_name_and_suggested() {
        List<String> messages;
        List<String> suggestions;

        try (CommandTester tester = tester()) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("mode creative", sender);
            suggestions = tester.suggest("mode s", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("CREATIVE");
        assertThat(suggestions).containsExactlyInAnyOrder("survival", "spectator");
    }

    @Test
    void unknown_name_is_rejected() {
        try (CommandTester tester = tester()) {
            FakeSender sender = FakeSender.player("Alice");
            // Vanilla's argument rejects it while parsing from 1.19.4; CommandLib rejects it when the command runs
            // before.
            //? if >=1.19.4 {
            assertThatThrownBy(() -> tester.execute("mode flying", sender)).isInstanceOf(RuntimeException.class);
            //?} else {
            /*tester.execute("mode flying", sender);
            assertThat(sender.getSentMessageTexts()).startsWith("Incorrect argument for command");
            *///?}
        }
    }
}
