package dev.marulabs.commandlib;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.SimplePluginManager;
import org.mockito.Mockito;
public class PermissionAuditProbe {
    public static void main(String[] args) throws Exception {
        Server server = Mockito.mock(Server.class);
        SimplePluginManager manager = new SimplePluginManager(server, Mockito.mock(SimpleCommandMap.class));
        Mockito.when(server.getPluginManager()).thenReturn(manager);
        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            bukkit.when(Bukkit::getServer).thenReturn(server);
            Permission old = new Permission("audit.shared", PermissionDefault.TRUE);
            Permission replacement = new Permission("audit.shared", PermissionDefault.FALSE);
            CommandPermissions.add(old);
            CommandPermissions.add(replacement);
            System.out.println("replacement permission is live => " + (manager.getPermission("audit.shared") == replacement));
            CommandPermissions.remove(old);
            System.out.println("after old permission removal => " + manager.getPermission("audit.shared"));
        }
    }
}
