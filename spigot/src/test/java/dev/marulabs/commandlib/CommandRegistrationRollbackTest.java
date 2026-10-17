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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CommandRegistrationRollbackTest {
    private final PluginManager pluginManager = mock(PluginManager.class);
    private final Server server = mock(Server.class);
    private final Plugin plugin = mock(Plugin.class);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        when(pluginManager.getPermissionSubscriptions(anyString())).thenReturn(Collections.emptySet());
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
}
