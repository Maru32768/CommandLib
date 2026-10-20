package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CommandRegistrationRollbackTest {
    private final PluginManager pluginManager = mock(PluginManager.class);
    private final Server server = mock(Server.class);
    private final Plugin plugin = mock(Plugin.class);
    private MockedStatic<Bukkit> bukkit;

    // What the plugin manager holds, by name, as Bukkit's does.
    private final Map<String, Permission> permissions = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(pluginManager.getPermissionSubscriptions(anyString())).thenReturn(Collections.emptySet());
        when(pluginManager.getPermission(anyString())).thenAnswer(x -> permissions.get(x.<String>getArgument(0)));
        doAnswer(x -> permissions.put(x.<Permission>getArgument(0)
                                       .getName(),
                                      x.getArgument(0))).when(pluginManager)
                                                        .addPermission(any(Permission.class));
        doAnswer(x -> permissions.remove(x.<String>getArgument(0))).when(pluginManager)
                                                                   .removePermission(anyString());
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(plugin.getName()).thenReturn("Test");
        when(plugin.getServer()).thenReturn(server);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServer)
              .thenReturn(server);
        bukkit.when(Bukkit::getPluginManager)
              .thenReturn(pluginManager);
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(true);
        bukkit.when(Bukkit::getOnlinePlayers)
              .thenReturn(List.of());
        // No NMS implementation matches this version, so enable() fails after the permissions are added.
        bukkit.when(Bukkit::getVersion)
              .thenReturn("git-Spigot (MC: 1.0)");
        bukkit.when(Bukkit::getBukkitVersion)
              .thenReturn("1.0-R0.1-SNAPSHOT");
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void a_failed_registration_removes_the_permissions_it_added() {
        assertThatThrownBy(() -> CommandLib.register(plugin,
                                                     new Command("spawn"))).isInstanceOf(RuntimeException.class);

        InOrder inOrder = inOrder(pluginManager);
        inOrder.verify(pluginManager)
               .addPermission(argThat((Permission x) -> x.getName()
                                                         .equals("test.command.spawn")));
        inOrder.verify(pluginManager)
               .removePermission("test.command.spawn");
        assertThat(CommandLib.diagnostics()
                             .registrations()).isEmpty();
    }

    @Test
    void a_declared_cooldown_bypass_node_is_neither_replaced_nor_removed() {
        Permission declared = new Permission("myplugin.admin");
        permissions.put("myplugin.admin", declared);

        assertThatThrownBy(() -> CommandLib.register(plugin,
                                                     cooldownCommand("myplugin.admin"))).isInstanceOf(RuntimeException.class);

        verify(pluginManager, never()).addPermission(argThat((Permission x) -> x.getName()
                                                                                .equals("myplugin.admin")));
        verify(pluginManager, never()).removePermission("myplugin.admin");
        assertThat(declared.getDefault()).isEqualTo(org.bukkit.permissions.PermissionDefault.OP);
    }

    @Test
    void an_undeclared_cooldown_bypass_node_is_added_and_removed() {
        assertThatThrownBy(() -> CommandLib.register(plugin,
                                                     cooldownCommand("myplugin.bypass"))).isInstanceOf(RuntimeException.class);

        InOrder inOrder = inOrder(pluginManager);
        inOrder.verify(pluginManager)
               .addPermission(argThat((Permission x) -> x.getName()
                                                         .equals("myplugin.bypass")));
        inOrder.verify(pluginManager)
               .removePermission("myplugin.bypass");
    }

    private static Command cooldownCommand(String bypassNode) {
        return new Command("spawn") {{
            cooldown(Cooldown.of(Duration.ofSeconds(5))
                             .bypassPermission(bypassNode));
            execute(ctx -> {
            });
        }};
    }
}
