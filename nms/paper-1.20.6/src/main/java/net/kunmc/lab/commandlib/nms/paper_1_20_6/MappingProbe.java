package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.CraftServer;

/**
 * Tells NMSClassRegistry whether this package matches the mappings the server loaded the plugin with. Paper remaps a
 * plugin that does not declare Mojang mappings from Spigot names to Mojang names, and the Spigot name MobEffect is the
 * Mojang name of MobEffectInstance. So the Mojang-named package sees MobEffect only when the plugin is not remapped,
 * and the reobfuscated package, which refers to the Spigot name MobEffectList, only when it is.
 *
 * <p>Spigot 1.20.5-1.20.6 also has a class named MobEffect, but keeps CraftBukkit in a versioned package. Loading the
 * unversioned CraftServer then throws NoClassDefFoundError, which NMSClassRegistry treats as not matching.
 */
final class MappingProbe {
    private MappingProbe() {
    }

    static boolean matches() {
        return "net.minecraft.world.effect.MobEffect".equals(MobEffect.class.getName())
                && CraftServer.class.getName() != null;
    }
}
