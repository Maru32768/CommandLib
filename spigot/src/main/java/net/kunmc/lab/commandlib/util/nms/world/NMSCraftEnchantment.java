package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.enchantments.Enchantment;

public abstract class NMSCraftEnchantment extends NMSClass {
    public static NMSCraftEnchantment create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftEnchantment.class))
                             .newInstance();
    }

    public NMSCraftEnchantment(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract Enchantment createInstance(NMSEnchantment nms);
}
