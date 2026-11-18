package dev.marulabs.cadence.internal.nms.resources;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.nms.core.NMSParticleParam;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.bukkit.Particle;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSCraftParticle extends NMSClass {
    public static NMSCraftParticle create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftParticle.class))
                             .newInstance();
    }

    public NMSCraftParticle(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract Particle toBukkit(NMSParticleParam nms);
}
