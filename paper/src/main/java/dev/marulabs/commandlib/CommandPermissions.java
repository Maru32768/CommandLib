package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The command permissions in Bukkit's plugin manager, and the defaults of those that bootstrap commands cannot register.
 */
final class CommandPermissions {
    // Bootstrap commands exist before the plugin manager does, so their permissions cannot be registered with it.
    private static final Map<String, PermissionDefault> BOOTSTRAP_DEFAULTS = new ConcurrentHashMap<>();
    private static final String NULL_SENDER_CLASS = "io.papermc.paper.brigadier.NullCommandSender";

    private CommandPermissions() {
    }

    static void addBootstrapDefaults(CommandSet commands) {
        commands.permissions()
                .forEach(p -> BOOTSTRAP_DEFAULTS.put(p.getName(), p.getDefault()));
    }

    /**
     * Checks a command permission, applying the default of a bootstrap command permission that nothing registered.
     */
    static boolean hasPermission(CommandSender sender, String name) {
        PermissionDefault bootstrapDefault = BOOTSTRAP_DEFAULTS.get(name);
        // The server is null while functions are first compiled, and Paper's sender for compiling functions on a
        // reload has every permission, like vanilla's NULL source.
        boolean useSender = Bukkit.getServer() == null || sender.getClass()
                                                                .getName()
                                                                .equals(NULL_SENDER_CLASS);
        if (bootstrapDefault == null || useSender || sender.isPermissionSet(name) || Bukkit.getPluginManager()
                                                                                           .getPermission(name) != null) {
            return sender.hasPermission(name);
        }
        return bootstrapDefault.getValue(sender.isOp());
    }

    /**
     * Adds the permission, replacing one of the same name.
     */
    static void add(Permission permission) {
        Permission existing = Bukkit.getPluginManager()
                                    .getPermission(permission.getName());
        if (existing != null) {
            remove(existing);
        }
        Bukkit.getPluginManager()
              .addPermission(permission);
    }

    /**
     * Removes the permission, after taking its default from the players who had it only by that default. A permission
     * that another registration has put under the same name since stays, as that registration still uses it.
     */
    static void remove(Permission permission) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (pluginManager.getPermission(permission.getName()) != permission) {
            return;
        }
        permission.setDefault(PermissionDefault.FALSE);
        pluginManager.recalculatePermissionDefaults(permission);
        pluginManager.removePermission(permission);
    }
}
