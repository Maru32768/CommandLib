package dev.marulabs.cadence.internal.nms.world;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSEnchantment extends NMSClass {
    public static NMSEnchantment create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSEnchantment.class), Object.class)
                             .newInstance(handle);
    }

    public NMSEnchantment(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
