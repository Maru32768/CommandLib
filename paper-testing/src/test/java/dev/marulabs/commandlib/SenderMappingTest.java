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
import java.util.function.Function;

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

    private static Command builder(Games games) {
        return Command.builder("team")
                      .provide(GamePlayer.class,
                               ctx -> games.of(Command.playerRequirer()
                                                      .extract(ctx)))
                      .addChild(Command.builder("show")
                                       .execute(ctx -> ctx.sendMessage(ctx.get(GamePlayer.class)
                                                                          .describe())))
                      .addChild(Command.builder("join")
                                       .argument(new StringArgument("team"), branch -> branch.execute((team, ctx) -> {
                                           GamePlayer player = ctx.get(GamePlayer.class);
                                           games.teams.put(ctx.getPlayer()
                                                              .getName(),
                                                           team);
                                           ctx.sendMessage(player.describe() + " joins " + team);
                                       })))
                      .build();
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
    void fluent_builder_and_annotated_commands_get_the_mapped_sender() {
        assertThat(run(SenderMappingTest::annotated)).isEqualTo(run(SenderMappingTest::fluent))
                                                     .isEqualTo(run(SenderMappingTest::builder))
                                                     .containsExactly("Steve of red",
                                                                      "Steve of red joins blue",
                                                                      "Steve of blue",
                                                                      "This command can only be executed by a player.");
    }

    private static List<String> run(Function<Games, Command> command) {
        Games games = games();
        FakeSender steve = FakeSender.player("Steve");
        FakeSender console = FakeSender.console();
        try (CommandTester tester = new CommandTester(() -> command.apply(games), "test.command")) {
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
