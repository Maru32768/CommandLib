package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
}
