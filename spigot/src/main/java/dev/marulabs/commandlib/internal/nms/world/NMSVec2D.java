package dev.marulabs.commandlib.internal.nms.world;

import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSVec2D extends NMSClass {
    public static NMSVec2D create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVec2D.class), Object.class)
                             .newInstance(handle);
    }

    public NMSVec2D(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract float x();

    public abstract float y();
}
