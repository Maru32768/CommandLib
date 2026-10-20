package dev.marulabs.commandlib.internal.nms.chat;

import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * for after 1.19.0
 */
@ApiStatus.Internal
public abstract class NMSTranslatableContents extends NMSClass {
    public static NMSTranslatableContents create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSTranslatableContents.class), Object.class)
                             .newInstance(handle);
    }

    public NMSTranslatableContents(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract String getKey();

    public abstract Object[] getArgs();
}
