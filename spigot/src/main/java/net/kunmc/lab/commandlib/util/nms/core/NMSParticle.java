package net.kunmc.lab.commandlib.util.nms.core;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSParticle extends NMSClass {
    public static NMSParticle create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSParticle.class), Object.class)
                             .newInstance(handle);
    }

    public NMSParticle(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
