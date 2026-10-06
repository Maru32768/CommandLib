package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Failure paths of arguments that resolve values through Bukkit registries or the scoreboard.
 */
class KeyedArgumentFailureTest {
    @Test
    void unknown_keys_send_failure_messages() {
        FakeSender sender = FakeSender.player("Alice");

        try (MockedStatic<Bukkit> ignored = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = CommandTester.builder()
                                                 .command(() -> new Command("advancement") {{
                                                     argument(new AdvancementArgument("key")).execute((value, ctx) -> ctx.sendMessage(
                                                             "unreachable"));
                                                 }})
                                                 .command(() -> new Command("loot") {{
                                                     argument(new LootTableArgument("key")).execute((value, ctx) -> ctx.sendMessage(
                                                             "unreachable"));
                                                 }})
                                                 .command(() -> new Command("recipe") {{
                                                     argument(new RecipeArgument("key")).execute((value, ctx) -> ctx.sendMessage(
                                                             "unreachable"));
                                                 }})
                                                 .permissionPrefix("test.command")
                                                 .build()) {
            tester.execute("advancement story/missing", sender);
            tester.execute("loot commandlib:missing", sender);
            tester.execute("recipe missing", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("story/missing is not a known advancement",
                                                                 "commandlib:missing is not a known loot table",
                                                                 "missing is not a known recipe");
    }

    @Test
    void invalid_namespaced_key_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (MockedStatic<Bukkit> ignored = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("recipe") {{
                 argument(new RecipeArgument("key")).execute((value, ctx) -> ctx.sendMessage("unreachable"));
             }}, "test.command")) {
            tester.execute("recipe Bad:Key", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Bad:Key is not valid namespaced key");
    }

    @Test
    void unknown_objective_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");
        ScoreboardManager manager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(manager.getMainScoreboard())
               .thenReturn(Mockito.mock(Scoreboard.class));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("score") {{
                 argument(new ObjectiveArgument("objective")).execute((value, ctx) -> ctx.sendMessage("unreachable"));
             }}, "test.command")) {
            bukkit.when(Bukkit::getScoreboardManager)
                  .thenReturn(manager);
            tester.execute("score kills", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("kills is not a known objective");
    }

    @Test
    void objective_argument_fails_when_scoreboard_is_unavailable() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("score") {{
            argument(new ObjectiveArgument("objective").scoreboard(() -> null)).execute((value, ctx) -> ctx.sendMessage(
                    "unreachable"));
        }}, "test.command")) {
            tester.execute("score kills", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("kills is not a known objective");
    }
}
