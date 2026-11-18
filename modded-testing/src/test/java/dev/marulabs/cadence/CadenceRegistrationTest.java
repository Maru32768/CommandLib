package dev.marulabs.cadence;

// Forge before 1.19 cannot register event listeners outside a real Forge launch.
//? if fabric || >=1.19 {
import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.cadence.argument.IntegerArgument;
import dev.marulabs.cadence.internal.CurrentServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Cadence.register while a server runs, which the tester does not use.
class CadenceRegistrationTest {
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    private final Commands commands = Mockito.mock(Commands.class);
    private final MinecraftServer server;

    CadenceRegistrationTest() {
        CommandTester.MinecraftBootstrap.ensureBootstrapped();
        server = Mockito.mock(MinecraftServer.class);
        Mockito.when(commands.getDispatcher())
               .thenReturn(dispatcher);
        Mockito.when(server.getCommands())
               .thenReturn(commands);
        Mockito.when(server.isSameThread())
               .thenReturn(true);
    }

    @Test
    void commands_registered_while_the_server_runs_reach_its_dispatcher_and_players() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        Mockito.when(player.getServer())
               .thenReturn(server);
        PlayerList players = Mockito.mock(PlayerList.class);
        Mockito.when(players.getPlayers())
               .thenReturn(List.of(player));
        Mockito.when(server.getPlayerList())
               .thenReturn(players);

        try (MockedStatic<CurrentServer> currentServer = Mockito.mockStatic(CurrentServer.class)) {
            currentServer.when(CurrentServer::get)
                         .thenReturn(server);
            Cadence.register("latetest", new Command("late"));
            Cadence.refresh(player);
        }

        assertThat(dispatcher.getRoot()
                             .getChild("late")).isNotNull();
        Mockito.verify(commands, Mockito.times(2))
               .sendCommands(player);
    }

    @Test
    void a_server_without_a_player_list_yet_gets_the_commands_only() {
        // Fabric reports the server from SERVER_STARTING, before it has a player list.
        try (MockedStatic<CurrentServer> currentServer = Mockito.mockStatic(CurrentServer.class)) {
            currentServer.when(CurrentServer::get)
                         .thenReturn(server);
            Cadence.register("startingtest", new Command("starting"));
            Cadence.refreshAll();
        }

        assertThat(dispatcher.getRoot()
                             .getChild("starting")).isNotNull();
        Mockito.verify(commands, Mockito.never())
               .sendCommands(Mockito.any());
    }

    @Test
    void colliding_top_level_names_throw_from_register_before_a_server_runs() {
        assertThatThrownBy(() -> Cadence.register("collisiontest",
                                                  new Command("same"),
                                                  new Command("same"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void an_invalid_definition_throws_from_register_before_its_defaults_are_kept() {
        Command broken = new Command("broken") {{
            permission(DefaultPermission.ALL);
            argument(new IntegerArgument("count").optional(1),
                     new IntegerArgument("amount")).execute((ctx, count, amount) -> {
                     });
        }};

        assertThatThrownBy(() -> Cadence.register("brokentest", broken)).isInstanceOf(IllegalStateException.class)
                                                                        .hasMessageContaining("optional argument 'count'");
        // An undeclared node needs level 4, which an ALL default kept by the failed registration would not.
        assertThat(Cadence.hasDefaultPermission(Mockito.mock(CommandSourceStack.class), "brokentest.broken")).isFalse();
    }

    @Test
    void a_node_registered_late_follows_its_default_and_an_undeclared_node_needs_level_4() {
        ServerPlayer op = Mockito.mock(ServerPlayer.class);
        Mockito.when(op.hasPermissions(4))
               .thenReturn(true);
        ServerPlayer guest = Mockito.mock(ServerPlayer.class);
        CommandSourceStack opSource = Mockito.mock(CommandSourceStack.class);
        Mockito.when(opSource.hasPermission(4))
               .thenReturn(true);
        CommandSourceStack guestSource = Mockito.mock(CommandSourceStack.class);

        try (MockedStatic<CurrentServer> currentServer = Mockito.mockStatic(CurrentServer.class)) {
            currentServer.when(CurrentServer::get)
                         .thenReturn(server);
            Cadence.register("defaulttest", new Command("ops"));
        }

        // Forge and NeoForge resolve a node that missed PermissionGatherEvent.Nodes by its default resolver.
        assertThat(Cadence.hasPermission(opSource, op, "defaulttest.ops")).isTrue();
        assertThat(Cadence.hasPermission(guestSource, guest, "defaulttest.ops")).isFalse();
        assertThat(Cadence.hasPermission(opSource, guest, "defaulttest.undeclared")).isTrue();
        assertThat(Cadence.hasPermission(guestSource, op, "defaulttest.undeclared")).isFalse();
    }

    @Test
    void a_cooldown_bypass_node_is_registered_with_the_command() {
        ServerPlayer op = Mockito.mock(ServerPlayer.class);
        Mockito.when(op.hasPermissions(4))
               .thenReturn(true);
        ServerPlayer guest = Mockito.mock(ServerPlayer.class);
        CommandSourceStack opSource = Mockito.mock(CommandSourceStack.class);
        Mockito.when(opSource.hasPermission(4))
               .thenReturn(true);
        CommandSourceStack guestSource = Mockito.mock(CommandSourceStack.class);

        try (MockedStatic<CurrentServer> currentServer = Mockito.mockStatic(CurrentServer.class)) {
            currentServer.when(CurrentServer::get)
                         .thenReturn(server);
            Cadence.register("cooldowntest", new Command("kit") {{
                cooldown(Cooldown.of(Duration.ofMinutes(1))
                                 .bypassPermission("cooldowntest.kit.bypass"));
            }});
        }

        // Judged by the node's OP default for the player, not by the source's level as an undeclared node would be.
        assertThat(Cadence.hasPermission(guestSource, op, "cooldowntest.kit.bypass")).isTrue();
        assertThat(Cadence.hasPermission(opSource, guest, "cooldowntest.kit.bypass")).isFalse();
    }

    @Test
    void a_cooldown_bypass_node_another_command_uses_keeps_that_commands_default() {
        ServerPlayer guest = Mockito.mock(ServerPlayer.class);
        CommandSourceStack guestSource = Mockito.mock(CommandSourceStack.class);

        try (MockedStatic<CurrentServer> currentServer = Mockito.mockStatic(CurrentServer.class)) {
            currentServer.when(CurrentServer::get)
                         .thenReturn(server);
            Cadence.register("bypasstest", new Command("kit") {{
                cooldown(Cooldown.of(Duration.ofMinutes(1))
                                 .bypassPermission("bypasstest.vip"));
            }}, new Command("vip") {{
                permission(DefaultPermission.ALL);
            }});
        }

        assertThat(Cadence.hasPermission(guestSource, guest, "bypasstest.vip")).isTrue();
    }
}
//?}
