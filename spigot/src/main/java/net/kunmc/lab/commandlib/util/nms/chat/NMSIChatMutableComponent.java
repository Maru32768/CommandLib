package net.kunmc.lab.commandlib.util.nms.chat;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

/**
 * for after 1.19.0
 */
public abstract class NMSIChatMutableComponent extends NMSClass {
    public static NMSIChatMutableComponent create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSIChatMutableComponent.class), Object.class)
                             .newInstance(handle);
    }

    public NMSIChatMutableComponent(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSTranslatableContents getContentsAsTranslatable();
}
