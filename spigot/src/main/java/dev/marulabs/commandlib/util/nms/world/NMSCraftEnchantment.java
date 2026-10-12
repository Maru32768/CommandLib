package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.Nullable;

public abstract class NMSCraftEnchantment extends NMSClass {
    public static NMSCraftEnchantment create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftEnchantment.class))
                             .newInstance();
    }

    public NMSCraftEnchantment(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract Enchantment createInstance(NMSEnchantment nms);
}
