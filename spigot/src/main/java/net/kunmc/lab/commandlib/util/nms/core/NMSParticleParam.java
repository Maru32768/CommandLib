package net.kunmc.lab.commandlib.util.nms.core;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSParticleParam extends NMSClass {
    public static NMSParticleParam create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSParticleParam.class), Object.class)
                             .newInstance(handle);
    }

    public NMSParticleParam(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSParticle getParticle();
}
