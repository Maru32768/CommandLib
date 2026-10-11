package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentTile extends NMSArgument<NMSArgumentTileLocation> {
    public static NMSArgumentTile create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTile.class))
                             .newInstance();
    }

    public NMSArgumentTile(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
