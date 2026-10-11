package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentNamespacedKey extends NMSArgument<String> {
    public static NMSArgumentNamespacedKey create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentNamespacedKey.class))
                             .newInstance();
    }

    public NMSArgumentNamespacedKey(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
