package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public abstract class NMSCraftPotionEffectType extends NMSClass {
    public static NMSCraftPotionEffectType create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftPotionEffectType.class))
                             .newInstance();
    }

    public static NMSCraftPotionEffectType create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftPotionEffectType.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCraftPotionEffectType(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract PotionEffectType createInstance(NMSMobEffectList nms);
}
