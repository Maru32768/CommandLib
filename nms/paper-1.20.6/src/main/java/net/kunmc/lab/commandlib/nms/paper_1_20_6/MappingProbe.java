package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.minecraft.world.effect.MobEffect;

/**
 * Tells NMSClassRegistry whether this package matches the mappings the server loaded the plugin with. Paper remaps a
 * plugin that does not declare Mojang mappings from Spigot names to Mojang names, and the Spigot name MobEffect is the
 * Mojang name of MobEffectInstance. So the Mojang-named package sees MobEffect only when the plugin is not remapped,
 * and the reobfuscated package, which refers to the Spigot name MobEffectList, only when it is.
 */
final class MappingProbe {
    private MappingProbe() {
    }

    static boolean matches() {
        return "net.minecraft.world.effect.MobEffect".equals(MobEffect.class.getName());
    }
}
