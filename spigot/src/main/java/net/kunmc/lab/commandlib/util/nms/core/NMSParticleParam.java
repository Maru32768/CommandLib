package net.kunmc.lab.commandlib.util.nms.core;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.core.v1_16_0.NMSParticleParam_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.core.v1_17_0.NMSParticleParam_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.core.v1_18_0.NMSParticleParam_v1_18_0;
import net.kunmc.lab.commandlib.util.nms.core.v1_20_5.NMSParticleParam_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSParticleParam extends MinecraftClass {
    public static NMSParticleParam create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSParticleParam.class), Object.class)
                             .newInstance(handle);
    }

    public NMSParticleParam(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract NMSParticle getParticle();

    static {
        NMSClassRegistry.register(NMSParticleParam.class, NMSParticleParam_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSParticleParam.class, NMSParticleParam_v1_17_0.class, "1.17.0", "1.17.1");
        NMSClassRegistry.register(NMSParticleParam.class, NMSParticleParam_v1_18_0.class, "1.18.0", "1.20.4");
        NMSClassRegistry.register(NMSParticleParam.class, NMSParticleParam_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSParticleParam_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSParticleParam_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSParticleParam_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSParticleParam_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSParticleParam_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSParticleParam_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSParticleParam_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSParticleParam_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSParticleParam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSParticleParam_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
