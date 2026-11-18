package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentNamespacedKey extends NMSArgument<String> {
    public static NMSArgumentNamespacedKey create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentNamespacedKey.class))
                             .newInstance();
    }

    public NMSArgumentNamespacedKey(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
