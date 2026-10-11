package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSItemStack extends NMSClass {
    public static NMSItemStack create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSItemStack.class), Object.class)
                             .newInstance(handle);
    }

    public NMSItemStack(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
