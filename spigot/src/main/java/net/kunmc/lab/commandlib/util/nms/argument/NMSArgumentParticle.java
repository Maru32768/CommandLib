package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentParticle_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentParticle_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_19_3.NMSArgumentParticle_v1_19_3;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentParticle_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentParticle extends NMSArgument<NMSParticleParam> {
    public static NMSArgumentParticle create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentParticle.class))
                             .newInstance();
    }

    public NMSArgumentParticle(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentParticle.class, NMSArgumentParticle_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentParticle.class, NMSArgumentParticle_v1_17_0.class, "1.17.0", "1.19.2");
        NMSClassRegistry.register(NMSArgumentParticle.class, NMSArgumentParticle_v1_19_3.class, "1.19.3", "1.20.4");
        NMSClassRegistry.register(NMSArgumentParticle.class, NMSArgumentParticle_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentParticle.class);
    }
}
