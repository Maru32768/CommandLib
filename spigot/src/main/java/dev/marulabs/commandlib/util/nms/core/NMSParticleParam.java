package dev.marulabs.commandlib.util.nms.core;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

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
