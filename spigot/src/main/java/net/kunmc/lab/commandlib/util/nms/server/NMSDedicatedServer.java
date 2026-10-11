package net.kunmc.lab.commandlib.util.nms.server;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSDedicatedServer extends NMSClass {
    public static NMSDedicatedServer create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSDedicatedServer.class), Object.class)
                             .newInstance(handle);
    }

    public NMSDedicatedServer(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSCommandDispatcher getCommandDispatcher();

    public abstract NMSDataPackResources getDataPackResources();
}
