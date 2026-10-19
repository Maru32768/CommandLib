package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.permission.Tristate;
import org.mockito.Mockito;

public class VelocityAuditProbe {
    public static void main(String[] args) throws Exception {
        var fixture = new CommandLibTest();
        fixture.setUp();
        var p = CommandLibTest.class.getDeclaredField("plugin");
        p.setAccessible(true);
        var s = CommandLibTest.class.getDeclaredField("proxy");
        s.setAccessible(true);
        Object plugin = p.get(fixture);
        ProxyServer proxy = (ProxyServer) s.get(fixture);
        Command old = new Command("audit");
        old.permission(DefaultPermission.ALL);
        CommandLib first = CommandLib.register(plugin, proxy, "audit", old);
        first.unregister();
        Command replacement = new Command("audit");
        replacement.permission(DefaultPermission.NONE);
        CommandLib second = CommandLib.register(plugin, proxy, "audit", replacement);
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getPermissionValue(Mockito.anyString())).thenReturn(Tristate.UNDEFINED);
        System.out.println("after unregister ALL and register NONE => allowed=" + VelocityPermissions.hasPermission(player, "audit.audit"));
        System.out.println("declared default => " + replacement.effectiveDefaultPermission());
        second.unregister();
    }
}
