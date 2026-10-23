package dev.marulabs.commandlib;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logs as the plugin that ships this copy of CommandLib, whose class loader loaded these classes, so the console names
 * that plugin.
 */
final class PluginLogger {
    private PluginLogger() {
    }

    static void error(String message, Throwable error) {
        logger().log(Level.SEVERE, message, error);
    }

    private static Logger logger() {
        try {
            return JavaPlugin.getProvidingPlugin(PluginLogger.class)
                             .getLogger();
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Not loaded by a plugin, as in the tests.
            return Logger.getLogger("dev.marulabs.commandlib");
        }
    }
}
