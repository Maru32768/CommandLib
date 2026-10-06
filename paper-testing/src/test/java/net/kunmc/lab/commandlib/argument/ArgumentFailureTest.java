package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Failure paths of Paper arguments, ported from the Spigot argument tests.
 */
class ArgumentFailureTest {
    enum Direction {
        NORTH,
        SOUTH
    }

    @Test
    void unknown_enum_and_object_values_send_red_incorrect_argument_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = CommandTester.builder()
                                                 .command(() -> new Command("go") {{
                                                     argument(new EnumArgument<>("dir", Direction.class)).execute((dir,
                                                                                                                   ctx) -> ctx.sendMessage(
                                                             "unreachable"));
                                                 }})
                                                 .command(() -> new Command("give") {{
                                                     argument(new ObjectArgument<>("item", Map.of("sword", 1))).execute((item,
                                                                                                                         ctx) -> ctx.sendMessage(
                                                             "unreachable"));
                                                 }})
                                                 .permissionPrefix("test.command")
                                                 .build()) {
            tester.execute("go up", sender);
            tester.execute("give axe", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("command.unknown.argument",
                                                                 "go up" + "command.context.here",
                                                                 "command.unknown.argument",
                                                                 "give axe" + "command.context.here");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .color()).isEqualTo(NamedTextColor.RED);
    }

    @Test
    void unknown_world_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
            argument(new WorldArgument("world")).execute((world, ctx) -> ctx.sendMessage("world=" + world));
        }}, "test.command")) {
            assertThatThrownBy(() -> tester.execute("tp missing", FakeSender.player("Alice"))).hasCauseInstanceOf(
                    CommandSyntaxException.class);
        }
    }

    @Test
    void unknown_team_sends_failure() {
        FakeSender sender = FakeSender.player("Alice");
        ScoreboardManager manager = Mockito.mock(ScoreboardManager.class);
        Mockito.when(manager.getMainScoreboard())
               .thenReturn(Mockito.mock(Scoreboard.class));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("team") {{
                 argument(new TeamArgument("team")).execute((team, ctx) -> ctx.sendMessage("unreachable"));
             }}, "test.command")) {
            bukkit.when(Bukkit::getScoreboardManager)
                  .thenReturn(manager);
            tester.execute("team blue", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("command.unknown.argument",
                                                                 "team blue" + "command.context.here");
    }

    @Test
    void unparsed_argument_has_no_placeholder_suggestions() {
        try (CommandTester tester = new CommandTester(() -> new Command("raw") {{
            argument(new UnparsedArgument("value")).execute((value, ctx) -> ctx.sendMessage(value));
        }}, "test.command")) {
            List<String> suggestions = tester.suggestions("raw ", FakeSender.player("Alice"))
                                             .join()
                                             .getList()
                                             .stream()
                                             .map(Suggestion::getText)
                                             .collect(Collectors.toList());

            assertThat(suggestions).doesNotContain("test", "sb@aaaa", "@a");
        }
    }

    @Test
    void unparsed_argument_reads_one_token_before_following_argument() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("raw") {{
            argument(new UnparsedArgument("value"), new StringArgument("next")).execute((value, next, ctx) -> {
                ctx.sendMessage(value + "|" + next);
            });
        }}, "test.command")) {
            tester.execute("raw @p[limit=1] tail", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("@p[limit=1]|tail");
    }
}
