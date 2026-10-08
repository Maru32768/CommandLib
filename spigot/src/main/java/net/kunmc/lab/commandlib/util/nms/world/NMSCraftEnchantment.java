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
        NMSClassRegistry.registerTyped(NMSCraftEnchantment.class);
    }
}
