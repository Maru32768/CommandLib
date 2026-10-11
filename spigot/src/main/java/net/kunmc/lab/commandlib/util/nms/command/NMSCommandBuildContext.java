package net.kunmc.lab.commandlib.util.nms.command;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

/**
 * for after 1.19.0
 */
public abstract class NMSCommandBuildContext extends NMSClass {
    public static NMSCommandBuildContext create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandBuildContext.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandBuildContext(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
