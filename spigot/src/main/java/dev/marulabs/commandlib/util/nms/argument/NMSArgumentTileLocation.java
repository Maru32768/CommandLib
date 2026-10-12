package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.world.NMSIBlockData;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentTileLocation extends NMSClass {
    public static NMSArgumentTileLocation create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTileLocation.class), Object.class)
                             .newInstance(handle);
    }

    public NMSArgumentTileLocation(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSIBlockData getBlockData();
}
