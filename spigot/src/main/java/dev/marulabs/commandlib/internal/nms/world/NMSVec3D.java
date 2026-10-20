package dev.marulabs.commandlib.internal.nms.world;

import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
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
