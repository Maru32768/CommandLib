package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentVec3D extends NMSArgument<NMSVec3D> {
    public static NMSArgumentVec3D create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentVec3D.class))
                             .newInstance();
    }

    public NMSArgumentVec3D(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
