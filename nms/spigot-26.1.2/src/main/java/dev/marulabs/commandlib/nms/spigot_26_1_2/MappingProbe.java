package dev.marulabs.commandlib.nms.spigot_26_1_2;

/**
 * Tells NMSClassRegistry whether this package runs on Spigot. Paper shares the Mojang names and CraftBukkit package
 * from 26.1 but registers commands its own way, which the paper module handles, so this package matches only where
 * Paper's lifecycle API is missing.
 */
final class MappingProbe {
    private MappingProbe() {
    }

    static boolean matches() {
        try {
            Class.forName("io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager",
                          false,
                          MappingProbe.class.getClassLoader());
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
