package net.kunmc.lab.commandlib.util.nms.resources;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Particle;

public abstract class NMSCraftParticle extends NMSClass {
    public static NMSCraftParticle create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftParticle.class))
                             .newInstance();
    }

    public NMSCraftParticle(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract Particle toBukkit(NMSParticleParam nms);
}
