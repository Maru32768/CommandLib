package dev.marulabs.commandlib;

import org.assertj.core.api.Assertions;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

class CommandPermissionTest {
    private static Command command(String name) {
        return new Command(name);
    }

    @Nested
    class PermissionName {
        @Test
        void returns_prefix_dot_name_by_default() {
            Command cmd = command("spawn");
            Assertions.assertThat(cmd.permissionName("myplugin.command"))
                      .isEqualTo("myplugin.command.spawn");
        }

        @Test
        void returns_custom_node_ignoring_prefix() {
            Command cmd = command("spawn");
            cmd.permission("myplugin.admin");
            Assertions.assertThat(cmd.permissionName("myplugin.command"))
                      .isEqualTo("myplugin.admin");
        }

        @Test
        void includes_parent_name_for_subcommand() {
            Command parent = command("game");
            Command child = command("start");
            parent.addChildren(child);
            Assertions.assertThat(child.permissionName("myplugin.command"))
                      .isEqualTo("myplugin.command.game.start");
        }

        @Test
        void includes_full_hierarchy_for_deeply_nested_subcommand() {
            Command root = command("game");
            Command mid = command("team");
            Command leaf = command("add");
            root.addChildren(mid);
            mid.addChildren(leaf);
            Assertions.assertThat(leaf.permissionName("myplugin.command"))
                      .isEqualTo("myplugin.command.game.team.add");
        }
    }

    @Nested
    class Permissions {
        private MockedStatic<Bukkit> mockedBukkit;

        @BeforeEach
        void setUp() {
            // Permission constructor calls Bukkit.getServer().getPluginManager().getPermissionSubscriptions()
            PluginManager mockPluginManager = Mockito.mock(PluginManager.class);
            Mockito.when(mockPluginManager.getPermissionSubscriptions(Mockito.anyString()))
                   .thenReturn(Collections.emptySet());
            Server mockServer = Mockito.mock(Server.class);
            Mockito.when(mockServer.getPluginManager())
                   .thenReturn(mockPluginManager);

            mockedBukkit = Mockito.mockStatic(Bukkit.class);
            mockedBukkit.when(Bukkit::getServer)
                        .thenReturn(mockServer);
        }

        @AfterEach
        void tearDown() {
            mockedBukkit.close();
        }

        @Test
        void includes_self() {
            Command cmd = command("spawn");
            List<Permission> perms = cmd.permissions("myplugin.command");
            Assertions.assertThat(perms)
                      .hasSize(1);
            Assertions.assertThat(perms.get(0)
                                       .getName())
                      .isEqualTo("myplugin.command.spawn");
        }

        @Test
        void includes_children_recursively() {
            Command root = command("game");
            Command start = command("start");
            Command stop = command("stop");
            root.addChildren(start, stop);

            List<Permission> perms = root.permissions("myplugin.command");
            Assertions.assertThat(perms.stream()
                                       .map(Permission::getName))
                      .containsExactlyInAnyOrder("myplugin.command.game",
                                                 "myplugin.command.game.start",
                                                 "myplugin.command.game.stop");
        }

        @Test
        void uses_op_default_by_default() {
            Command cmd = command("spawn");
            Permission perm = cmd.permissions("myplugin.command")
                                 .get(0);
            Assertions.assertThat(perm.getDefault())
                      .isEqualTo(PermissionDefault.OP);
        }

        @Test
        void uses_custom_default_when_set() {
            Command cmd = command("spawn");
            cmd.permission(PermissionDefault.TRUE);
            Permission perm = cmd.permissions("myplugin.command")
                                 .get(0);
            Assertions.assertThat(perm.getDefault())
                      .isEqualTo(PermissionDefault.TRUE);
        }

        @Test
        void uses_custom_node_and_default_when_set() {
            Command cmd = command("spawn");
            cmd.permission("myplugin.admin", PermissionDefault.FALSE);
            Permission perm = cmd.permissions("myplugin.command")
                                 .get(0);
            Assertions.assertThat(perm.getName())
                      .isEqualTo("myplugin.admin");
            Assertions.assertThat(perm.getDefault())
                      .isEqualTo(PermissionDefault.FALSE);
        }
    }
}
