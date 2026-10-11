package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TeamArgumentTest {
    @Test
    void custom_scoreboard_is_resolved_on_each_parse_and_suggestion() {
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
                             .join()
                             .getList()).extracting(Suggestion::getText)
                                        .containsExactly("arena_red");

            current.set(second);
            tester.execute("team arena_red", sender);
            assertThat(tester.suggestions("team arena", sender)
                             .join()
                             .getList()).isEmpty();

            current.set(null);
            tester.execute("team arena_red", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("arena_red",
                                                                 "command.unknown.argument",
                                                                 "team arena_red" + "command.context.here",
                                                                 "command.unknown.argument",
                                                                 "team arena_red" + "command.context.here");
    }
}
