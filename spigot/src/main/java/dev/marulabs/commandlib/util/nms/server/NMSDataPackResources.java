package dev.marulabs.commandlib.util.nms.server;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.command.NMSCommandBuildContext;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

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
