package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.md_5.bungee.api.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LiteralArgumentTest {
    @Test
    void listed_literal_is_accepted() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(modeCommand(List.of("on", "off")), "test.command")) {
            tester.execute("mode off", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("mode=off");
    }

    @Test
    void unlisted_literal_sends_red_incorrect_argument_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(modeCommand(List.of("on", "off")), "test.command")) {
            tester.execute("mode auto", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "mode auto<--[HERE]");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .getColor()).isEqualTo(ChatColor.RED);
    }

    @Test
    void suggestions_list_literals_matching_input() {
        try (CommandTester tester = new CommandTester(modeCommand(List.of("on", "off", "auto")), "test.command")) {
            // Literal suggestions match by substring, so "o" also matches "auto".
            assertThat(tester.suggest("mode of", FakeSender.player("Alice"))).containsExactly("off");
            assertThat(tester.suggest("mode o", FakeSender.player("Alice"))).containsExactlyInAnyOrder("on",
                                                                                                       "off",
                                                                                                       "auto");
        }
    }

    @Test
    void supplier_literals_are_read_on_each_execution() {
        List<String> literals = new ArrayList<>(List.of("on"));
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("mode") {{
            argument(new LiteralArgument("value",
                                         () -> literals)).execute((ctx, value) -> ctx.sendMessage("mode=" + value));
        }}, "test.command")) {
            literals.add("off");
            tester.execute("mode off", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("mode=off");
    }

    private static Command modeCommand(List<String> literals) {
        return new Command("mode") {{
            argument(new LiteralArgument("value", literals)).execute((ctx,
                                                                      value) -> ctx.sendMessage("mode=" + value));
        }};
    }
}
