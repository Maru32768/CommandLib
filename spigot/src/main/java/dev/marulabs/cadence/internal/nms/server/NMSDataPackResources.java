package dev.marulabs.cadence.internal.nms.server;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.nms.command.NMSCommandBuildContext;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSDataPackResources extends NMSClass {
    public static NMSDataPackResources create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSDataPackResources.class), Object.class)
                             .newInstance(handle);
    }

    public NMSDataPackResources(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    /**
     * for after 1.19.0
     */
    public abstract NMSCommandBuildContext getCommandBuildContext();
}
