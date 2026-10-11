package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.potion.PotionEffectType;

public abstract class NMSCraftPotionEffectType extends NMSClass {
    public static NMSCraftPotionEffectType create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftPotionEffectType.class))
                             .newInstance();
    }

    public static NMSCraftPotionEffectType create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftPotionEffectType.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCraftPotionEffectType(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract PotionEffectType createInstance(NMSMobEffectList nms);
}
