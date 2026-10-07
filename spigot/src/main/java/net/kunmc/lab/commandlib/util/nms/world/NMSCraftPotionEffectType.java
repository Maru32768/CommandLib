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
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSCraftPotionEffectType_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSCraftPotionEffectType_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSCraftPotionEffectType_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSCraftPotionEffectType_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSCraftPotionEffectType_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSCraftPotionEffectType_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSCraftPotionEffectType_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSCraftPotionEffectType_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSCraftPotionEffectType.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSCraftPotionEffectType_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
