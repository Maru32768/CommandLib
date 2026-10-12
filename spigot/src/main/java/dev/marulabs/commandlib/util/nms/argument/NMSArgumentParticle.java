package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentParticle extends NMSArgument<NMSParticleParam> {
    public static NMSArgumentParticle create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentParticle.class))
                             .newInstance();
    }

    public NMSArgumentParticle(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
