package net.kunmc.lab.commandlib.util.nms.chat;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.chat.v1_19_0.NMSIChatMutableComponent_v1_19_0;
import net.kunmc.lab.commandlib.util.nms.chat.v1_20_5.NMSIChatMutableComponent_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

/**
 * for after 1.19.0
 */
public abstract class NMSIChatMutableComponent extends MinecraftClass {
    public static NMSIChatMutableComponent create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSIChatMutableComponent.class), Object.class)
                             .newInstance(handle);
    }

    public NMSIChatMutableComponent(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract NMSTranslatableContents getContentsAsTranslatable();

    static {
        NMSClassRegistry.register(NMSIChatMutableComponent.class,
                                  NMSIChatMutableComponent_v1_19_0.class,
                                  "1.19.0",
                                  "1.20.4");
        NMSClassRegistry.register(NMSIChatMutableComponent.class,
                                  NMSIChatMutableComponent_v1_20_5.class,
                                  "1.20.5",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSIChatMutableComponent.class);
    }
}
