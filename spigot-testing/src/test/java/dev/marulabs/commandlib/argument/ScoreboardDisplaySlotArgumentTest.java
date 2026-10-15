package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("SIDEBAR", "PLAYER_LIST", "BELOW_NAME");
    }

    @Test
    void team_slots_of_spigot_and_paper_get_vanilla_names() {
        assertThat(ScoreboardDisplaySlotArgument.vanillaName("SIDEBAR_DARK_RED")).isEqualTo("sidebar.team.dark_red");
        assertThat(ScoreboardDisplaySlotArgument.vanillaName("SIDEBAR_TEAM_DARK_RED")).isEqualTo("sidebar.team.dark_red");
    }

    @Test
    void bukkit_enum_name_is_a_syntax_error() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ScoreboardDisplaySlotArgumentTest::command, "test.command")) {
            assertThatThrownBy(() -> tester.execute("slot player_list",
                                                    sender)).hasMessageContaining("Unknown display slot 'player_list'");
        }

        assertThat(sender.getSentMessageTexts()).isEmpty();
    }

    @Test
    void team_slot_fails_where_bukkit_has_no_team_slots() {
        FakeSender sender = FakeSender.player("Alice");

        // The Bukkit API of the tests, 1.16.5, has no team slots, while the mocked vanilla type accepts them.
        try (CommandTester tester = new CommandTester(ScoreboardDisplaySlotArgumentTest::command, "test.command")) {
            tester.execute("slot sidebar.team.red", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("sidebar.team.red is not a display slot of this server");
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
