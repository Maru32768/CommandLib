package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSVec3D extends NMSClass {
    public static NMSVec3D create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVec3D.class), Object.class)
                             .newInstance(handle);
    }

    public NMSVec3D(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract double x();

    public abstract double y();

    public abstract double z();
}
