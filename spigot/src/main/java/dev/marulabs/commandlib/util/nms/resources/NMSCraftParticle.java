package dev.marulabs.commandlib.util.nms.resources;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Particle;
import org.jetbrains.annotations.Nullable;

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
