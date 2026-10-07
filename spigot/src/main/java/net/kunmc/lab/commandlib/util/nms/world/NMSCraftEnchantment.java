package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.CraftBukkitClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSCraftEnchantment_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_20_4.NMSCraftEnchantment_v1_20_4;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.enchantments.Enchantment;

public abstract class NMSCraftEnchantment extends CraftBukkitClass {
    public static NMSCraftEnchantment create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftEnchantment.class))
                             .newInstance();
    }

    public NMSCraftEnchantment(Object handle, String className) {
        super(handle, className);
    }

    public abstract Enchantment createInstance(NMSEnchantment nms);

    static {
        NMSClassRegistry.register(NMSCraftEnchantment.class, NMSCraftEnchantment_v1_16_0.class, "1.16.0", "1.20.3");
        NMSClassRegistry.register(NMSCraftEnchantment.class, NMSCraftEnchantment_v1_20_4.class, "1.20.4", "9.9.9");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSCraftEnchantment_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSCraftEnchantment_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSCraftEnchantment_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSCraftEnchantment_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSCraftEnchantment_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSCraftEnchantment_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSCraftEnchantment_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSCraftEnchantment_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSCraftEnchantment_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
