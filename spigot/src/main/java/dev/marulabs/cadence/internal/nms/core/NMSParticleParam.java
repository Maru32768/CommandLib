package dev.marulabs.cadence.internal.nms.core;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSParticleParam extends NMSClass {
    public static NMSParticleParam create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSParticleParam.class), Object.class)
                             .newInstance(handle);
    }

    public NMSParticleParam(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSParticle getParticle();
}
