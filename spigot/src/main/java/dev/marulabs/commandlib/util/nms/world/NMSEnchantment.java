package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSEnchantment extends NMSClass {
    public static NMSEnchantment create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSEnchantment.class), Object.class)
                             .newInstance(handle);
    }

    public NMSEnchantment(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
