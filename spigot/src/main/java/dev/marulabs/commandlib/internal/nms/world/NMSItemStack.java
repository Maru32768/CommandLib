package dev.marulabs.commandlib.internal.nms.world;

import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSItemStack extends NMSClass {
    public static NMSItemStack create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSItemStack.class), Object.class)
                             .newInstance(handle);
    }

    public NMSItemStack(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
