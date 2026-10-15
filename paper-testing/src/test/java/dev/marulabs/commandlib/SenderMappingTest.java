package dev.marulabs.commandlib;

import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import dev.marulabs.commandlib.annotation.Dependency;
import dev.marulabs.commandlib.annotation.Subcommand;
import dev.marulabs.commandlib.argument.StringArgument;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// The same test runs in spigot-testing.
class SenderMappingTest {
    // A plugin's own sender type, wrapping the player.
    static final class GamePlayer {
        private final Player player;
        private final String team;

        GamePlayer(Player player, String team) {
            this.player = player;
            this.team = team;
        }

        String describe() {
            return player.getName() + " of " + team;
        }
    }

    static final class Games {
        private final Map<String, String> teams = new HashMap<>();

        GamePlayer of(Player player) {
            return new GamePlayer(player, teams.getOrDefault(player.getName(), "none"));
        }
    }

    private static Games games() {
        Games games = new Games();
        games.teams.put("Steve", "red");
        return games;
    }

    private static Command fluent(Games games) {
        return new Command("team") {{
            provide(GamePlayer.class, ctx -> games.of(playerRequirer().extract(ctx)));
            addChildren(new Command("show") {{
                execute(ctx -> ctx.sendMessage(ctx.get(GamePlayer.class)
                                                  .describe()));
            }}, new Command("join") {{
                argument(new StringArgument("team")).execute((team, ctx) -> {
                    GamePlayer player = ctx.get(GamePlayer.class);
                    games.teams.put(ctx.getPlayer()
                                       .getName(),
                                    team);
                    ctx.sendMessage(player.describe() + " joins " + team);
                });
            }});
        }};
    }

    @dev.marulabs.commandlib.annotation.Command("team")
    static final class AnnotatedTeamCommand {
        @Dependency
        Games games;

        @Subcommand("show")
        void show(CommandContext ctx, GamePlayer player) {
            ctx.sendMessage(player.describe());
        }

        @Subcommand("join")
        void join(CommandContext ctx, GamePlayer player, String team) {
            games.teams.put(ctx.getPlayer()
                               .getName(),
                            team);
            ctx.sendMessage(player.describe() + " joins " + team);
        }
    }

    private static Command annotated(Games games) {
        return AnnotatedCommands.parser()
                                .registerDependency(Games.class, games)
                                .registerContextValue(GamePlayer.class,
                                                      ctx -> games.of(Command.playerRequirer()
                                                                             .extract(ctx)))
                                .parse(new AnnotatedTeamCommand());
    }

    @Test
    void fluent_and_annotated_commands_get_the_mapped_sender() {
        assertThat(run(true)).isEqualTo(run(false))
                             .containsExactly("Steve of red",
                                              "Steve of red joins blue",
                                              "Steve of blue",
                                              "This command can only be executed by a player.");
    }

    private static List<String> run(boolean annotated) {
        Games games = games();
        FakeSender steve = FakeSender.player("Steve");
        FakeSender console = FakeSender.console();
        try (CommandTester tester = new CommandTester(() -> annotated ? annotated(games) : fluent(games),
                                                      "test.command")) {
            tester.execute("team show", steve);
            tester.execute("team join blue", steve);
            tester.execute("team show", steve);
            tester.execute("team show", console);
        }
        List<String> messages = new ArrayList<>(steve.getSentMessageTexts());
        messages.addAll(console.getSentMessageTexts());
        return messages;
    }
}
