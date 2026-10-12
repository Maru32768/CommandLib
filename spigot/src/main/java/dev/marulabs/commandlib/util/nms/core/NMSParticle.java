package dev.marulabs.commandlib.util.nms.core;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSParticle extends NMSClass {
    public static NMSParticle create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSParticle.class), Object.class)
                             .newInstance(handle);
    }

    public NMSParticle(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
