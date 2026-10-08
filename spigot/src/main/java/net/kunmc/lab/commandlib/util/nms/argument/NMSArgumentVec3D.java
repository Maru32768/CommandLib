package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentVec3D_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentVec3D_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentVec3D_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentVec3D extends NMSArgument<NMSVec3D> {
    public static NMSArgumentVec3D create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentVec3D.class))
                             .newInstance();
    }

    public NMSArgumentVec3D(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentVec3D.class, NMSArgumentVec3D_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentVec3D.class, NMSArgumentVec3D_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSArgumentVec3D.class, NMSArgumentVec3D_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentVec3D.class);
    }
}
