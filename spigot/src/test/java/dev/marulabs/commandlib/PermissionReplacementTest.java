package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.SimplePluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Bukkit's own permission registry, in which a permission is replaced and removed by its name.
class PermissionReplacementTest {
    private final Server server = mock(Server.class);
    private final SimplePluginManager pluginManager = new SimplePluginManager(server, null);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        when(server.getPluginManager()).thenReturn(pluginManager);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServer)
              .thenReturn(server);
        bukkit.when(Bukkit::getPluginManager)
              .thenReturn(pluginManager);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void removing_an_older_registrations_permission_keeps_the_one_that_replaced_it() {
        Permission older = new Permission("test.command.spawn", PermissionDefault.TRUE);
        Permission newer = new Permission("test.command.spawn", PermissionDefault.FALSE);
        CommandLib.addPermission(older);
        CommandLib.addPermission(newer);

        CommandLib.removePermissions(List.of(older));

        assertThat(pluginManager.getPermission("test.command.spawn")).isSameAs(newer);
        assertThat(newer.getDefault()).isEqualTo(PermissionDefault.FALSE);
        assertThat(pluginManager.getDefaultPermissions(false)).doesNotContain(older);

        CommandLib.removePermissions(List.of(newer));

        assertThat(pluginManager.getPermission("test.command.spawn")).isNull();
    }
}
