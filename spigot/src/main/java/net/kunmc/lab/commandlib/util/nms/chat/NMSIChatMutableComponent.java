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
        NMSClassRegistry.registerTyped(NMSIChatMutableComponent.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSIChatMutableComponent_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSIChatMutableComponent.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSIChatMutableComponent_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSIChatMutableComponent.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSIChatMutableComponent_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSIChatMutableComponent.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSIChatMutableComponent_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSIChatMutableComponent.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSIChatMutableComponent_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSIChatMutableComponent.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSIChatMutableComponent_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
    }
}
