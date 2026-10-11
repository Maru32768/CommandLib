package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

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
