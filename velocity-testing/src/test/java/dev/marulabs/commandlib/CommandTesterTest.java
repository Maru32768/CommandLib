package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.LiteralArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.ServerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.util.text.RichText;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandTesterTest {
    @Test
    void builder_defaults_wrap_every_command_and_provide_values() {
        java.util.List<String> log = new java.util.ArrayList<>();
        CommandTester tester = CommandTester.builder()
                                            .command(new Command("a") {{
                                                execute(ctx -> log.add("a " + ctx.get(String.class)));
                                            }})
                                            .command(new Command("b") {{
                                                provide(String.class, ctx -> "b");
                                                execute(ctx -> log.add("b " + ctx.get(String.class)));
                                            }})
                                            .permissionPrefix("proxy.command")
                                            .defaults(CommandDefaults.of(CommandContext.class)
                                                                     .addMiddleware((ctx, next) -> {
                                                                         log.add("audit " + ctx.getActor()
                                                                                               .getName());
                                                                         return next.proceed();
                                                                     })
                                                                     .provide(String.class, ctx -> "default"))
                                            .build();
        FakeSender sender = FakeSender.player("Steve");

        tester.execute("a", sender);
        tester.execute("b", sender);

        assertThat(log).containsExactly("audit Steve", "a default", "audit Steve", "b b");
    }

    @Test
    void primitive_arguments_parse_and_messages_are_recorded() {
        CommandTester tester = new CommandTester(new Command("add") {{
            argument(new IntegerArgument("a"), new IntegerArgument("b")).execute((a, b, ctx) -> {
                ctx.sendSuccess(a + b);
                ctx.setResult(a + b);
            });
        }}, "proxy.command");
        FakeSender sender = FakeSender.player("Alice");

        assertThat(tester.execute("add 2 3", sender)).isEqualTo(5);
        assertThat(sender.getSentMessageTexts()).containsExactly("5");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .color()).isEqualTo(NamedTextColor.GREEN);
    }

    @Test
    void player_argument_finds_online_players_ignoring_case_and_suggests_them() {
        List<Player> targets = new ArrayList<>();
        CommandTester tester = new CommandTester(new Command("find") {{
            argument(new PlayerArgument("target")).execute((target, ctx) -> targets.add(target));
        }}, "proxy.command");
        FakeSender alice = FakeSender.player("Alice");
        tester.withFakePlayer(alice)
              .withFakePlayer(FakeSender.player("Bob"));

        tester.execute("find alice", alice);

        assertThat(targets).containsExactly((Player) alice.asSender());
        // The generated help node is suggested as well.
        assertThat(tester.suggest("find ", alice)).containsExactly("Alice", "Bob", "help");
        assertThat(tester.suggest("find b", alice)).containsExactly("Bob");
    }

    @Test
    void unknown_player_fails_with_incorrect_argument() {
        CommandTester tester = new CommandTester(new Command("find") {{
            argument(new PlayerArgument("target")).execute((target, ctx) -> ctx.sendMessage("found"));
        }}, "proxy.command");
        FakeSender sender = FakeSender.console();

        assertThat(tester.execute("find nobody", sender)).isZero();
        assertThat(sender.getSentMessageTexts()).containsExactly("command.unknown.argument",
                                                                 "find nobodycommand.context.here");
    }

    @Test
    void server_argument_finds_registered_servers() {
        List<RegisteredServer> targets = new ArrayList<>();
        CommandTester tester = new CommandTester(new Command("send") {{
            argument(new ServerArgument("server")).execute((server, ctx) -> targets.add(server));
        }}, "proxy.command");
        RegisteredServer lobby = tester.withFakeServer("lobby");
        tester.withFakeServer("survival");
        FakeSender sender = FakeSender.player("Alice");

        tester.execute("send Lobby", sender);

        assertThat(targets).containsExactly(lobby);
        assertThat(tester.suggest("send ", sender)).containsExactly("help", "lobby", "survival");
    }

    @Test
    void server_argument_suggests_a_name_that_needs_quotes_quoted() {
        List<RegisteredServer> targets = new ArrayList<>();
        CommandTester tester = new CommandTester(new Command("send") {{
            argument(new ServerArgument("server")).execute((server, ctx) -> targets.add(server));
        }}, "proxy.command");
        RegisteredServer myLobby = tester.withFakeServer("my lobby");
        tester.withFakeServer("lobby");
        FakeSender sender = FakeSender.player("Alice");

        assertThat(tester.suggest("send ", sender)).containsExactly("\"my lobby\"", "help", "lobby");
        assertThat(tester.suggest("send my", sender)).containsExactly("\"my lobby\"");
        assertThat(tester.suggest("send \"my lo", sender)).containsExactly("\"my lobby\"");
        assertThat(tester.suggest("send \"lob", sender)).containsExactly("\"my lobby\"", "lobby");

        tester.execute("send \"My Lobby\"", sender);

        assertThat(targets).containsExactly(myLobby);
    }

    @Test
    void literal_argument_suggests_a_literal_that_needs_quotes_quoted() {
        List<String> chosen = new ArrayList<>();
        List<String> drinks = List.of("red wine", "beer");
        CommandTester tester = new CommandTester(new Command("pick") {{
            argument(new LiteralArgument("choice", drinks)).execute((choice, ctx) -> chosen.add(choice));
        }}, "proxy.command");
        FakeSender sender = FakeSender.player("Alice");

        assertThat(tester.suggest("pick \"red", sender)).containsExactly("\"red wine\"");

        tester.execute("pick \"red wine\"", sender);

        assertThat(chosen).containsExactly("red wine");
    }

    @Test
    void routes_need_the_permission_of_every_step() {
        CommandTester tester = new CommandTester(new Command("config") {{
            argument(new StringArgument("key")).execute((key, ctx) -> ctx.sendMessage(key));
        }}, "proxy.command");

        FakeSender allowed = FakeSender.player("Alice")
                                       .permissions("proxy.command.config", "proxy.command.config.key");
        tester.execute("config motd", allowed);
        assertThat(allowed.getSentMessageTexts()).containsExactly("motd");

        FakeSender denied = FakeSender.player("Bob")
                                      .denyPermissions("proxy.command.config.key");
        assertThatThrownBy(() -> tester.execute("config motd",
                                                denied)).hasCauseInstanceOf(CommandSyntaxException.class);
        assertThat(tester.suggest("con", denied)).containsExactly("config");
    }

    @Test
    void unset_permissions_fall_back_to_the_command_defaults() {
        CommandTester tester = new CommandTester(new Command("vote") {{
            permission(DefaultPermission.ALL);
            execute(ctx -> ctx.sendMessage("voted"));
            addChildren(new Command("close") {{
                permission(DefaultPermission.OP);
                execute(ctx -> ctx.sendMessage("closed"));
            }});
        }}, "proxy.command");
        // No permission plugin: every node is unset.
        FakeSender player = FakeSender.player("Alice")
                                      .permissions();

        tester.execute("vote", player);
        assertThatThrownBy(() -> tester.execute("vote close", player)).hasCauseInstanceOf(CommandSyntaxException.class);
        player.permissions("proxy.command.vote.close", "proxy.command.vote.close");
        tester.execute("vote close", player);

        assertThat(player.getSentMessageTexts()).containsExactly("voted", "closed");
    }

    @Test
    void each_tester_applies_its_own_defaults() {
        CommandTester open = new CommandTester(new Command("vote") {{
            permission(DefaultPermission.ALL);
            execute(ctx -> ctx.sendMessage("voted"));
        }}, "proxy.command");
        CommandTester closed = new CommandTester(new Command("vote") {{
            execute(ctx -> ctx.sendMessage("voted"));
        }}, "proxy.command");
        FakeSender player = FakeSender.player("Alice")
                                      .permissions();

        open.execute("vote", player);
        assertThatThrownBy(() -> closed.execute("vote", player)).hasCauseInstanceOf(CommandSyntaxException.class);

        assertThat(player.getSentMessageTexts()).containsExactly("voted");
    }

    @Test
    void granted_and_denied_permissions_combine_and_a_new_grant_list_keeps_the_denied_nodes() {
        FakeSender sender = FakeSender.player("Alice")
                                      .permissions("a.b", "c.d")
                                      .denyPermissions("c.d");

        assertThat(sender.asSender()
                         .getPermissionValue("a.b")).isEqualTo(Tristate.TRUE);
        assertThat(sender.asSender()
                         .getPermissionValue("c.d")).isEqualTo(Tristate.FALSE);
        assertThat(sender.asSender()
                         .getPermissionValue("e.f")).isEqualTo(Tristate.UNDEFINED);

        sender.permissions("c.d", "e.f");

        assertThat(sender.asSender()
                         .getPermissionValue("e.f")).isEqualTo(Tristate.TRUE);
        assertThat(sender.asSender()
                         .getPermissionValue("c.d")).isEqualTo(Tristate.FALSE);
        assertThat(sender.asSender()
                         .getPermissionValue("a.b")).isEqualTo(Tristate.UNDEFINED);
    }

    @Test
    void duplicate_permission_nodes_are_accepted() {
        CommandTester tester = new CommandTester(new Command("hub") {{
            execute(ctx -> ctx.sendMessage("hub"));
        }}, "proxy.command");
        FakeSender denied = FakeSender.player("Alice")
                                      .denyPermissions("proxy.command.hub", "proxy.command.hub");

        assertThatThrownBy(() -> tester.execute("hub", denied)).hasCauseInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void require_player_rejects_the_console() {
        CommandTester tester = new CommandTester(new Command("hub") {{
            requirePlayer();
            execute(ctx -> ctx.sendMessage("sent"));
        }}, "proxy.command");
        FakeSender console = FakeSender.console();

        assertThat(tester.execute("hub", console)).isZero();
        assertThat(console.getSentMessageTexts()).containsExactly("This command can only be executed by a player.");
    }

    @Test
    void context_reads_the_player_locale_and_sends_rich_text() {
        CommandTester tester = new CommandTester(new Command("lang") {{
            execute(ctx -> ctx.sendMessage(RichText.text(ctx.getLanguage())
                                                   .bold()));
        }}, "proxy.command");
        FakeSender sender = FakeSender.player("Alice")
                                      .locale(Locale.JAPAN);

        tester.execute("lang", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("ja_jp");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .hasDecoration(TextDecoration.BOLD)).isTrue();
    }
}
