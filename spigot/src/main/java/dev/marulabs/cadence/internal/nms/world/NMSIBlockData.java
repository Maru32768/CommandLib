package dev.marulabs.cadence.internal.nms.world;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class NMSIBlockData extends NMSClass {
    public static NMSIBlockData create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSIBlockData.class), Object.class)
                             .newInstance(handle);
    }

    public NMSIBlockData(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
