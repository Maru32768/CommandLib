package dev.marulabs.commandlib;

// Forge before 1.19 cannot register event listeners outside a real Forge launch.
//? if fabric || >=1.19 {
import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.commandlib.util.CurrentServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// CommandLib.register while a server runs, which the tester does not use.
class CommandLibRegistrationTest {
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    private final Commands commands = Mockito.mock(Commands.class);
    private final MinecraftServer server;

    CommandLibRegistrationTest() {
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
            CommandLib.register("latetest", new Command("late"));
            CommandLib.refresh(player);
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
            CommandLib.register("startingtest", new Command("starting"));
            CommandLib.refreshAll();
        }

        assertThat(dispatcher.getRoot()
                             .getChild("starting")).isNotNull();
        Mockito.verify(commands, Mockito.never())
               .sendCommands(Mockito.any());
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
            CommandLib.register("defaulttest", new Command("ops"));
        }

        // Forge and NeoForge resolve a node that missed PermissionGatherEvent.Nodes by its default resolver.
        assertThat(CommandLib.hasPermission(opSource, op, "defaulttest.ops")).isTrue();
        assertThat(CommandLib.hasPermission(guestSource, guest, "defaulttest.ops")).isFalse();
        assertThat(CommandLib.hasPermission(opSource, guest, "defaulttest.undeclared")).isTrue();
        assertThat(CommandLib.hasPermission(guestSource, op, "defaulttest.undeclared")).isFalse();
    }
}
//?}
