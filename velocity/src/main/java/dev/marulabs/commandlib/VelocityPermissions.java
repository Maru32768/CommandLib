package dev.marulabs.commandlib;

import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Velocity has no registry of permissions with defaults, so CommandLib applies the default of a command permission
 * that no permission plugin sets.
 */
final class VelocityPermissions {
    // Replaced as a whole when a registration is added or removed.
    private static volatile Map<String, DefaultPermission> defaults = Map.of();
    // Set by the tester from its first command until it closes, so testers in one JVM do not share defaults.
    static volatile Map<String, DefaultPermission> testerDefaults = Map.of();

    private VelocityPermissions() {
    }

    /**
     * Sets the defaults of the registrations that are not unregistered, oldest first. The first of them that declares
     * a node keeps its default, as on modded servers, so a later one cannot open an {@code OP} node to everyone.
     */
    static void setDefaults(List<PermissionConfig> configs) {
        Map<String, DefaultPermission> map = new HashMap<>();
        configs.forEach(c -> map.putIfAbsent(c.node(), c.defaultPermission()));
        defaults = map;
    }

    /**
     * A value the subject has for the node wins. Otherwise only {@link DefaultPermission#ALL} grants it, since the
     * proxy has no operators.
     */
    static boolean hasPermission(PermissionSubject subject, String node) {
        Tristate value = subject.getPermissionValue(node);
        if (value != Tristate.UNDEFINED) {
            return value.asBoolean();
        }
        return testerDefaults.getOrDefault(node, defaults.get(node)) == DefaultPermission.ALL;
    }
}
