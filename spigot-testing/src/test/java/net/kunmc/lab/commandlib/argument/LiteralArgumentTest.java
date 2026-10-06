package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import net.md_5.bungee.api.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                         .getColor()).isEqualTo(ChatColor.of(new java.awt.Color(0xFF5555)));
    }

    @Test
    void suggestions_list_literals_matching_input() {
        try (CommandTester tester = new CommandTester(modeCommand(List.of("on", "off", "auto")), "test.command")) {
            // Literal suggestions match by substring, so "o" also matches "auto".
            assertThat(suggest(tester, "mode of")).containsExactly("off");
            assertThat(suggest(tester, "mode o")).containsExactlyInAnyOrder("on", "off", "auto");
        }
    }

    @Test
    void supplier_literals_are_read_on_each_execution() {
        List<String> literals = new ArrayList<>(List.of("on"));
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("mode") {{
            argument(new LiteralArgument("value", () -> literals)).execute((value, ctx) -> ctx.sendMessage("mode=" + value));
        }}, "test.command")) {
            literals.add("off");
            tester.execute("mode off", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("mode=off");
    }

    private static Command modeCommand(List<String> literals) {
        return new Command("mode") {{
            argument(new LiteralArgument("value", literals)).execute((value, ctx) -> ctx.sendMessage("mode=" + value));
        }};
    }

    private static List<String> suggest(CommandTester tester, String input) {
        return tester.suggestions(input, FakeSender.player("Alice"))
                     .join()
                     .getList()
                     .stream()
                     .map(com.mojang.brigadier.suggestion.Suggestion::getText)
                     .collect(Collectors.toList());
    }
}
