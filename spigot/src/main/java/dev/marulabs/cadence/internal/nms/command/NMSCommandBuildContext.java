package dev.marulabs.cadence.internal.nms.command;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * for after 1.19.0
 */
@ApiStatus.Internal
public abstract class NMSCommandBuildContext extends NMSClass {
    public static NMSCommandBuildContext create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandBuildContext.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandBuildContext(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
