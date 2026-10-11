package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentParticle extends NMSArgument<NMSParticleParam> {
    public static NMSArgumentParticle create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentParticle.class))
                             .newInstance();
    }

    public NMSArgumentParticle(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
