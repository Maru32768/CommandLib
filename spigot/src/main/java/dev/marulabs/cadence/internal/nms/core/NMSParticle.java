package dev.marulabs.cadence.internal.nms.core;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSParticle extends NMSClass {
    public static NMSParticle create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSParticle.class), Object.class)
                             .newInstance(handle);
    }

    public NMSParticle(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
