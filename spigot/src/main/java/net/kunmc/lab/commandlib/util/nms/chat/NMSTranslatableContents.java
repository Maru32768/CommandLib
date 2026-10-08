package net.kunmc.lab.commandlib.util.nms.chat;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.chat.v1_19_0.NMSTranslatableContents_v1_19_0;
import net.kunmc.lab.commandlib.util.nms.chat.v1_20_4.NMSTranslatableContents_v1_20_4;
import net.kunmc.lab.commandlib.util.nms.chat.v1_20_5.NMSTranslatableContents_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

/**
 * for after 1.19.0
 */
public abstract class NMSTranslatableContents extends MinecraftClass {
    public static NMSTranslatableContents create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSTranslatableContents.class), Object.class)
                             .newInstance(handle);
    }

    public NMSTranslatableContents(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract String getKey();

    public abstract Object[] getArgs();

    static {
        NMSClassRegistry.register(NMSTranslatableContents.class,
                                  NMSTranslatableContents_v1_19_0.class,
                                  "1.19.0",
                                  "1.20.3");
        NMSClassRegistry.register(NMSTranslatableContents.class,
                                  NMSTranslatableContents_v1_20_4.class,
                                  "1.20.4",
                                  "1.20.4");
        NMSClassRegistry.register(NMSTranslatableContents.class,
                                  NMSTranslatableContents_v1_20_5.class,
                                  "1.20.5",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSTranslatableContents.class);
    }
}
