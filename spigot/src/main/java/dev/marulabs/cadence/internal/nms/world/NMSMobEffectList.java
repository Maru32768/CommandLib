package dev.marulabs.cadence.internal.nms.world;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSMobEffectList extends NMSClass {
    public static NMSMobEffectList create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSMobEffectList.class), Object.class)
                             .newInstance(handle);
    }

    public NMSMobEffectList(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
