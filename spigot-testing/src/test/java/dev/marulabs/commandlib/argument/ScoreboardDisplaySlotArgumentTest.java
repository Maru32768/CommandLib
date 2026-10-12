package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreboardDisplaySlotArgumentTest {
    @Test
    void display_slot_is_parsed_case_insensitively() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("slot") {{
            argument(new ScoreboardDisplaySlotArgument("value")).execute((slot,
                                                                          ctx) -> ctx.sendMessage(slot.name()));
        }}, "test.command")) {
            tester.execute("slot sidebar", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("SIDEBAR");
    }
}
