package net.kunmc.lab.commandlib.nms.spigot_1_21_1;

/**
 * Tells NMSClassRegistry whether this package runs on Spigot. Paper shares the version range from 1.20.5, and its
 * remapper translates the Spigot names in this package's bytecode, but not the Spigot class names that the wrappers
 * look up by string. So the package matches only where the Spigot name MobEffectList is a class.
 */
final class MappingProbe {
    private MappingProbe() {
    }

    static boolean matches() {
        try {
            Class.forName("net.minecraft.world.effect.MobEffectList", false, MappingProbe.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
