package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSEnchantment extends NMSClass {
    public static NMSEnchantment create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSEnchantment.class), Object.class)
                             .newInstance(handle);
    }

    public NMSEnchantment(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
