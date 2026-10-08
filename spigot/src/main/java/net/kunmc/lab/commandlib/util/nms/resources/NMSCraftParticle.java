package net.kunmc.lab.commandlib.util.nms.resources;

import net.kunmc.lab.commandlib.util.nms.CraftBukkitClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.kunmc.lab.commandlib.util.nms.resources.v1_16_0.NMSCraftParticle_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.resources.v1_20_2.NMSCraftParticle_v1_20_2;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Particle;

public abstract class NMSCraftParticle extends CraftBukkitClass {
    public static NMSCraftParticle create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftParticle.class))
                             .newInstance();
    }

    public NMSCraftParticle(Object handle, String className) {
        super(handle, className);
    }

    public abstract Particle toBukkit(NMSParticleParam nms);

    static {
        NMSClassRegistry.register(NMSCraftParticle.class, NMSCraftParticle_v1_16_0.class, "1.16.0", "1.20.1");
        NMSClassRegistry.register(NMSCraftParticle.class, NMSCraftParticle_v1_20_2.class, "1.20.2", "9.9.9");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class);
    }
}
