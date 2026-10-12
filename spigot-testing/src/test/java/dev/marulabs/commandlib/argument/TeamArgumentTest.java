package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TeamArgumentTest {
    @Test
    void team_name_resolves_to_scoreboard_team() {
        FakeSender sender = FakeSender.player("Alice");
        Team mockTeam = Mockito.mock(Team.class);
        Mockito.when(mockTeam.getName())
               .thenReturn("red");
        Scoreboard mockScoreboard = Mockito.mock(Scoreboard.class);
        Mockito.when(mockScoreboard.getTeam("red"))
               .thenReturn(mockTeam);
        ScoreboardManager mockManager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(mockManager.getMainScoreboard())
               .thenReturn(mockScoreboard);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("team") {{
                 argument(new TeamArgument("name")).execute((team, ctx) -> {
                     ctx.sendMessage(team.getName());
                 });
             }}, "test.command")) {
            bukkit.when(Bukkit::getScoreboardManager)
                  .thenReturn(mockManager);
            tester.execute("team red", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("red");
    }

    @Test
    void unknown_team_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");
        Scoreboard mockScoreboard = Mockito.mock(Scoreboard.class);
        ScoreboardManager mockManager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(mockManager.getMainScoreboard())
               .thenReturn(mockScoreboard);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("team") {{
                 argument(new TeamArgument("name")).execute((team, ctx) -> {
                     ctx.sendMessage(team.getName());
                 });
             }}, "test.command")) {
            bukkit.when(Bukkit::getScoreboardManager)
                  .thenReturn(mockManager);
            tester.execute("team blue", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Unknown team 'blue'");
    }
    @Test
    void custom_scoreboard_is_resolved_on_each_parse_and_suggestion() throws Exception {
        FakeSender sender = FakeSender.player("Alice");
        Team arena = Mockito.mock(Team.class);
        Mockito.when(arena.getName())
               .thenReturn("arena_red");
        Scoreboard first = Mockito.mock(Scoreboard.class);
        Mockito.when(first.getTeam("arena_red"))
               .thenReturn(arena);
        Mockito.when(first.getTeams())
               .thenReturn(Set.of(arena));
        Scoreboard second = Mockito.mock(Scoreboard.class);
        AtomicReference<Scoreboard> current = new AtomicReference<>(first);

        // No Bukkit static mock: the main scoreboard must not be touched.
        try (CommandTester tester = new CommandTester(() -> new Command("team") {{
            argument(new TeamArgument("name").scoreboard(current::get)).execute((team, ctx) -> {
                ctx.sendMessage(team.getName());
            });
        }}, "test.command")) {
            tester.execute("team arena_red", sender);
            assertThat(tester.suggestions("team arena", sender)
                             .get()
                             .getList()).extracting(Suggestion::getText)
                                        .containsExactly("arena_red");

            current.set(second);
            tester.execute("team arena_red", sender);
            assertThat(tester.suggestions("team arena", sender)
                             .get()
                             .getList()).isEmpty();

            current.set(null);
            tester.execute("team arena_red", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("arena_red",
                                                                 "Incorrect argument for command",
                                                                 "team arena_red<--[HERE]",
                                                                 "Incorrect argument for command",
                                                                 "team arena_red<--[HERE]");
    }
}
