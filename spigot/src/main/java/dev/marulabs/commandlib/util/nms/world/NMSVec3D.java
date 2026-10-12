package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSVec3D extends NMSClass {
    public static NMSVec3D create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVec3D.class), Object.class)
                             .newInstance(handle);
    }

    public NMSVec3D(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract double x();

    public abstract double y();

    public abstract double z();
}
