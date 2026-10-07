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
        NMSClassRegistry.register(NMSCraftParticle.class, NMSCraftParticle_v1_16_0.class, "1.16.0", "1.20.3");
        NMSClassRegistry.register(NMSCraftParticle.class, NMSCraftParticle_v1_20_2.class, "1.20.4", "9.9.9");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSCraftParticle_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSCraftParticle_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSCraftParticle_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSCraftParticle_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSCraftParticle_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSCraftParticle_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSCraftParticle_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSCraftParticle_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSCraftParticle.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSCraftParticle_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
