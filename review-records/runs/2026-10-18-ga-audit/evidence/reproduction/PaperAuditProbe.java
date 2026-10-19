package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.Plugin;
import org.mockito.Mockito;

public class PaperAuditProbe {
    public static void main(String[] args) throws Exception {
        var fixture = new CommandLibLifecycleTest();
        fixture.setUp();
        try {
            var field = CommandLibLifecycleTest.class.getDeclaredField("plugin");
            field.setAccessible(true);
            Plugin plugin = (Plugin) field.get(fixture);
            Command broken = new Command("broken");
            broken.argument(new dev.marulabs.commandlib.argument.IntegerArgument("optional").optional(1),
                            new dev.marulabs.commandlib.argument.IntegerArgument("required")).execute((a, b, ctx) -> {});
            CommandLib lib = CommandLib.register(plugin, "audit", broken);
            System.out.println("invalid definition register returned => " + (lib != null));
            var fire = CommandLibLifecycleTest.class.getDeclaredMethod("fireCommandsEvent");
            fire.setAccessible(true);
            var dispatcher = (com.mojang.brigadier.CommandDispatcher<?>) fire.invoke(fixture);
            System.out.println("invalid definition label after event => " + dispatcher.getRoot().getChild("broken"));
            lib.unregister();
        } finally {
            fixture.tearDown();
        }
    }
}
