package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_3;

/**
 * Tells NMSClassRegistry whether this package runs on Spigot. Paper shares the version range from 1.20.5 and has
 * modules of its own, which the integration tests run there, so this package matches only where the Spigot name
 * MobEffectList is a class.
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
