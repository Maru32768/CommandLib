package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.CraftBukkitClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSCraftPotionEffectType_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_20_4.NMSCraftPotionEffectType_v1_20_4;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.potion.PotionEffectType;

public abstract class NMSCraftPotionEffectType extends CraftBukkitClass {
    public static NMSCraftPotionEffectType create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftPotionEffectType.class))
                             .newInstance();
    }

    public static NMSCraftPotionEffectType create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftPotionEffectType.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCraftPotionEffectType(Object handle, String className) {
        super(handle, className);
    }

    public abstract PotionEffectType createInstance(NMSMobEffectList nms);

    static {
        NMSClassRegistry.register(NMSCraftPotionEffectType.class,
                                  NMSCraftPotionEffectType_v1_16_0.class,
                                  "1.16.0",
                                  "1.20.3");
        NMSClassRegistry.register(NMSCraftPotionEffectType.class,
                                  NMSCraftPotionEffectType_v1_20_4.class,
                                  "1.20.4",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class);
    }
}
