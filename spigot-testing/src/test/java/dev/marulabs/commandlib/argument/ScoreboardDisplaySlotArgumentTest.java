package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreboardDisplaySlotArgumentTest {
    private static Command command() {
        return new Command("slot") {{
            argument(new ScoreboardDisplaySlotArgument("value")).execute((slot,
                                                                          ctx) -> ctx.sendMessage(slot.name()));
        }};
    }

    @Test
    void display_slot_is_parsed_by_vanilla_name() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ScoreboardDisplaySlotArgumentTest::command, "test.command")) {
            tester.execute("slot sidebar", sender);
            tester.execute("slot list", sender);
            tester.execute("slot below_name", sender);
            tester.execute("slot belowName", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("SIDEBAR", "PLAYER_LIST", "BELOW_NAME", "BELOW_NAME");
    }

    @Test
    void team_slots_of_spigot_and_paper_get_vanilla_names() {
        assertThat(ScoreboardDisplaySlotArgument.vanillaName("SIDEBAR_DARK_RED")).isEqualTo("sidebar.team.dark_red");
        assertThat(ScoreboardDisplaySlotArgument.vanillaName("SIDEBAR_TEAM_DARK_RED")).isEqualTo("sidebar.team.dark_red");
    }

    @Test
    void bukkit_enum_name_is_rejected() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ScoreboardDisplaySlotArgumentTest::command, "test.command")) {
            tester.execute("slot player_list", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "slot player_list<--[HERE]");
    }

    @Test
    void suggestions_are_vanilla_names() throws Exception {
        try (CommandTester tester = new CommandTester(ScoreboardDisplaySlotArgumentTest::command, "test.command")) {
            var suggestions = tester.suggestions("slot ", FakeSender.player("Alice"))
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(Suggestion::getText)
                                   .contains("list", "sidebar", "below_name")
                                   .doesNotContain("player_list");
        }
    }
}
