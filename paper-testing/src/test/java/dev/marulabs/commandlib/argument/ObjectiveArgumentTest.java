package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectiveArgumentTest {
    private static final Component KILLS_NOT_FOUND = Component.translatable("arguments.objective.notFound",
                                                                            Component.text("kills"))
                                                              .color(NamedTextColor.RED);

    @Test
    void objective_is_resolved_by_name_on_the_main_scoreboard() {
        AtomicReference<Objective> received = new AtomicReference<>();
        Objective kills = objective("kills");
        Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
        Mockito.when(scoreboard.getObjective("kills"))
               .thenReturn(kills);

        try (MockedStatic<Bukkit> bukkit = mainScoreboard(scoreboard);
             CommandTester tester = new CommandTester(() -> new Command("score") {{
                 argument(new ObjectiveArgument("objective")).execute((value, ctx) -> received.set(value));
             }}, "test.command")) {
            tester.execute("score kills", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(kills);
    }

    @Test
    void unknown_objective_sends_vanilla_not_found_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (MockedStatic<Bukkit> ignored = mainScoreboard(Mockito.mock(Scoreboard.class));
             CommandTester tester = new CommandTester(() -> new Command("score") {{
                 argument(new ObjectiveArgument("objective")).execute((value,
                                                                       ctx) -> ctx.sendMessage("unreachable"));
             }}, "test.command")) {
            tester.execute("score kills", sender);
        }

        assertThat(sender.getSentMessages()).containsExactly(KILLS_NOT_FOUND);
    }

    @Test
    void missing_scoreboard_sends_not_found_message_and_no_suggestions() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("score") {{
            argument(new ObjectiveArgument("objective").scoreboard(() -> null)).execute((value,
                                                                                         ctx) -> ctx.sendMessage("unreachable"));
        }}, "test.command")) {
            tester.execute("score kills", sender);
            assertThat(tester.suggest("score k", sender)).isEmpty();
        }

        assertThat(sender.getSentMessages()).containsExactly(KILLS_NOT_FOUND);
    }

    @Test
    void suggestions_are_objective_names_containing_the_input_ignoring_case() {
        Set<Objective> objectives = Set.of(objective("kills"), objective("deaths"), objective("arena_KILLS"));
        Scoreboard scoreboard = Mockito.mock(Scoreboard.class);
        Mockito.when(scoreboard.getObjectives())
               .thenReturn(objectives);
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("score") {{
            argument(new ObjectiveArgument("objective").scoreboard(() -> scoreboard)).execute((value, ctx) -> {
            });
        }}, "test.command")) {
            assertThat(tester.suggest("score Kill", sender)).containsExactlyInAnyOrder("kills", "arena_KILLS");
            assertThat(tester.suggest("score ", sender)).contains("kills", "deaths", "arena_KILLS");
        }
    }

    private static Objective objective(String name) {
        Objective objective = Mockito.mock(Objective.class);
        Mockito.when(objective.getName())
               .thenReturn(name);
        return objective;
    }

    private static MockedStatic<Bukkit> mainScoreboard(Scoreboard scoreboard) {
        ScoreboardManager manager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(manager.getMainScoreboard())
               .thenReturn(scoreboard);
        MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScoreboardManager)
              .thenReturn(manager);
        return bukkit;
    }
}
