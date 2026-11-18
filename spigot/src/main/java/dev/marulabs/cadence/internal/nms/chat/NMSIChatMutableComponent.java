package dev.marulabs.cadence.internal.nms.chat;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * for after 1.19.0
 */
@ApiStatus.Internal
public abstract class NMSIChatMutableComponent extends NMSClass {
    public static NMSIChatMutableComponent create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSIChatMutableComponent.class), Object.class)
                             .newInstance(handle);
    }

    public NMSIChatMutableComponent(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSTranslatableContents getContentsAsTranslatable();
}
