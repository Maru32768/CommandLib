package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;

public class NMSIBlockData extends NMSClass {
    public static NMSIBlockData create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSIBlockData.class), Object.class)
                             .newInstance(handle);
    }

    public NMSIBlockData(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
