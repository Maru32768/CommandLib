package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectiveArgumentTest {
    @Test
    void objective_is_resolved_by_name() {
        FakeSender sender = FakeSender.player("Alice");
        Objective objective = Mockito.mock(Objective.class);
        Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
        Mockito.when(scoreboard.getObjective("kills"))
               .thenReturn(objective);
        ScoreboardManager scoreboardManager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(scoreboardManager.getMainScoreboard())
               .thenReturn(scoreboard);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScoreboardManager)
                  .thenReturn(scoreboardManager);
            try (CommandTester tester = new CommandTester(new Command("objective") {{
                argument(new ObjectiveArgument("value")).execute((ctx, value) -> ctx.sendMessage("ok"));
            }}, "test.command")) {
                tester.execute("objective kills", sender);
            }
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ok");
    }

    @Test
    void suggestions_include_objective_names() throws Exception {
        Objective kills = Mockito.mock(Objective.class);
        Mockito.when(kills.getName())
               .thenReturn("kills");
        Objective deaths = Mockito.mock(Objective.class);
        Mockito.when(deaths.getName())
               .thenReturn("deaths");
        Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
        Mockito.when(scoreboard.getObjectives())
               .thenReturn(java.util.Set.of(kills, deaths));
        ScoreboardManager scoreboardManager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(scoreboardManager.getMainScoreboard())
               .thenReturn(scoreboard);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScoreboardManager)
                  .thenReturn(scoreboardManager);
            try (CommandTester tester = new CommandTester(new Command("objective") {{
                argument(new ObjectiveArgument("value")).execute((ctx, value) -> {
                });
            }}, "test.command")) {
                var suggestions = tester.suggestions("objective ki", FakeSender.player("Alice"))
                                        .get()
                                        .getList();

                assertThat(suggestions).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                       .containsExactly("kills");
            }
        }
    }

    @Test
    void custom_scoreboard_is_used_for_parse_and_suggestions() throws Exception {
        FakeSender sender = FakeSender.player("Alice");
        Objective objective = Mockito.mock(Objective.class);
        Mockito.when(objective.getName())
               .thenReturn("arena_kills");
        Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
        Mockito.when(scoreboard.getObjective("arena_kills"))
               .thenReturn(objective);
        Mockito.when(scoreboard.getObjectives())
               .thenReturn(java.util.Set.of(objective));

        try (CommandTester tester = new CommandTester(new Command("objective") {{
            argument(new ObjectiveArgument("value").scoreboard(() -> scoreboard)).execute((ctx,
                                                                                           value) -> ctx.sendMessage(value.getName()));
        }}, "test.command")) {
            tester.execute("objective arena_kills", sender);
            var suggestions = tester.suggestions("objective arena", FakeSender.player("Bob"))
                                    .get()
                                    .getList();

            assertThat(sender.getSentMessageTexts()).containsExactly("arena_kills");
            assertThat(suggestions).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                   .containsExactly("arena_kills");
        }
    }
}
