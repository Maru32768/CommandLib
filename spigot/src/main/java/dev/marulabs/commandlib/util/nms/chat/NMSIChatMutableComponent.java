package dev.marulabs.commandlib.util.nms.chat;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

/**
 * for after 1.19.0
 */
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
