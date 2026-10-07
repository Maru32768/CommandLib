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
        NMSClassRegistry.registerTyped(NMSTranslatableContents.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSTranslatableContents_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSTranslatableContents.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSTranslatableContents_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSTranslatableContents.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSTranslatableContents_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSTranslatableContents.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSTranslatableContents_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSTranslatableContents.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSTranslatableContents_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSTranslatableContents.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSTranslatableContents_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
    }
}
