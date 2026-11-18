package dev.marulabs.cadence.internal.nms.world;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
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
