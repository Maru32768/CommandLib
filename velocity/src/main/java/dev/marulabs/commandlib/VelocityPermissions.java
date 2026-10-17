package dev.marulabs.commandlib;

import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity has no registry of permissions with defaults, so CommandLib applies the default of a command permission
 * that no permission plugin sets.
 */
final class VelocityPermissions {
    private static final Map<String, DefaultPermission> DEFAULTS = new ConcurrentHashMap<>();

    private VelocityPermissions() {
    }

    /**
     * The first registration of a node keeps its default, as on modded servers, so a later one cannot open an
     * {@code OP} node to everyone.
     */
    static void addDefaults(Collection<PermissionConfig> configs) {
        configs.forEach(c -> DEFAULTS.putIfAbsent(c.node(), c.defaultPermission()));
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
        return DEFAULTS.get(node) == DefaultPermission.ALL;
    }
}
