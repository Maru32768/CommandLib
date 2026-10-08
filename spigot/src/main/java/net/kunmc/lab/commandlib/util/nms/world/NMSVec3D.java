package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSVec3D_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_17_0.NMSVec3D_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_19_0.NMSVec3D_v1_19_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_20_5.NMSVec3D_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSVec3D extends MinecraftClass {
    public static NMSVec3D create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVec3D.class), Object.class)
                             .newInstance(handle);
    }

    public NMSVec3D(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract double x();

    public abstract double y();

    public abstract double z();

    static {
        NMSClassRegistry.register(NMSVec3D.class, NMSVec3D_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSVec3D.class, NMSVec3D_v1_17_0.class, "1.17.0", "1.18.2");
        NMSClassRegistry.register(NMSVec3D.class, NMSVec3D_v1_19_0.class, "1.19.0", "1.20.4");
        NMSClassRegistry.register(NMSVec3D.class, NMSVec3D_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSVec3D.class);
    }
}
