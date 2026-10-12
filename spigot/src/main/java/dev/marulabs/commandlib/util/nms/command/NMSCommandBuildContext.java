package dev.marulabs.commandlib.util.nms.command;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

/**
 * for after 1.19.0
 */
public abstract class NMSCommandBuildContext extends NMSClass {
    public static NMSCommandBuildContext create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandBuildContext.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandBuildContext(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
