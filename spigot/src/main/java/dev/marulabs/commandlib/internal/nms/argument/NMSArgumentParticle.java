package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentParticle extends NMSArgument<NMSParticleParam> {
    public static NMSArgumentParticle create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentParticle.class))
                             .newInstance();
    }

    public NMSArgumentParticle(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
