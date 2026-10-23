package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs the vanilla time parser of every node.
class TimeArgumentTest {
    private static CommandTester tester(int minimum) {
        return new CommandTester(() -> new Command("wait") {{
            argument(new TimeArgument("ticks", minimum)).execute((ctx, ticks) -> {
                ctx.sendMessage(String.valueOf(ticks));
            });
        }}, "test.command");
    }

    @Test
    void ticks_seconds_and_days_are_read_as_game_ticks() {
        List<String> messages;

        try (CommandTester tester = tester(0)) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("wait 20", sender);
            tester.execute("wait 10s", sender);
            tester.execute("wait 0.5d", sender);
            assertThatThrownBy(() -> tester.execute("wait 1h", sender)).hasMessageContaining("Invalid unit");
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("20", "200", "12000");
    }

    @Test
    void fewer_ticks_than_the_minimum_are_rejected() {
        List<String> messages;

        try (CommandTester tester = tester(20)) {
            FakeSender sender = FakeSender.player("Alice");
            //? if >=1.19.4 {
            // "Tick count ..." before 1.21, "The tick count ..." from then.
            assertThatThrownBy(() -> tester.execute("wait 10",
                                                    sender)).hasMessageContaining("ick count must not be less than 20, found 10");
            //?} else
            /*tester.execute("wait 10", sender);*/
            tester.execute("wait 1s", sender);
            messages = sender.getSentMessageTexts();
        }

        //? if >=1.19.4 {
        assertThat(messages).containsExactly("20");
        //?} else
        /*assertThat(messages).containsExactly("Tick count must not be less than 20, found 10", "20");*/
    }

    @Test
    void units_are_suggested_after_a_number() throws Exception {
        try (CommandTester tester = tester(0)) {
            var suggestions = tester.suggestions("wait 5", FakeSender.player("Alice"))
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(Suggestion::getText)
                                   .contains("d", "s", "t");
        }
    }
}
