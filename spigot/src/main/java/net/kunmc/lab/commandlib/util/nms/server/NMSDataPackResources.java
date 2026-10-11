package net.kunmc.lab.commandlib.util.nms.server;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandBuildContext;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSDataPackResources extends NMSClass {
    public static NMSDataPackResources create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSDataPackResources.class), Object.class)
                             .newInstance(handle);
    }

    public NMSDataPackResources(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    /**
     * for after 1.19.0
     */
    public abstract NMSCommandBuildContext getCommandBuildContext();
}
