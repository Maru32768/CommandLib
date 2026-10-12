package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSItemStack extends NMSClass {
    public static NMSItemStack create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSItemStack.class), Object.class)
                             .newInstance(handle);
    }

    public NMSItemStack(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
