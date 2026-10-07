package net.kunmc.lab.commandlib.util.nms.core;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.core.v1_19_0.NMSHolder_v1_19_0;
import net.kunmc.lab.commandlib.util.nms.core.v1_20_5.NMSHolder_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;


/**
 * for after 1.19.0
 */
public abstract class NMSHolder extends MinecraftClass {
    public static NMSHolder create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSHolder.class), Object.class)
                             .newInstance(handle);
    }

    public NMSHolder(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSHolder.class, NMSHolder_v1_19_0.class, "1.19.0", "1.20.4");
        NMSClassRegistry.register(NMSHolder.class, NMSHolder_v1_20_5.class, "1.20.5", "9.9.9");
    }

    public static abstract class NMSReference extends MinecraftClass {
        public static NMSReference create(Object handle) {
            return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSReference.class), Object.class)
                                 .newInstance(handle);
        }

        public NMSReference(Object handle, String className, String... classNames) {
            super(handle, className, classNames);
        }

        public abstract Object value();

        static {
            NMSClassRegistry.register(NMSReference.class,
                                      NMSHolder_v1_19_0.NMSReference_v1_19_0.class,
                                      "1.19.0",
                                      "1.20.4");
            NMSClassRegistry.register(NMSReference.class,
                                      NMSHolder_v1_20_5.NMSReference_v1_20_5.class,
                                      "1.20.5",
                                      "9.9.9");
            NMSClassRegistry.registerTyped(NMSReference.class,
                                           "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSReference_paper_1_20_6",
                                           "1.20.5",
                                           "1.20.6");
            NMSClassRegistry.registerTyped(NMSReference.class,
                                           "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSReference_spigot_1_20_1",
                                           "1.20.1",
                                           "1.20.1");
            NMSClassRegistry.registerTyped(NMSReference.class,
                                           "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSReference_spigot_1_19_4",
                                           "1.19.4",
                                           "1.19.4");
            NMSClassRegistry.registerTyped(NMSReference.class,
                                           "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSReference_spigot_1_19_2",
                                           "1.19.2",
                                           "1.19.2");
            NMSClassRegistry.registerTyped(NMSReference.class,
                                           "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSReference_paper_1_20_6",
                                           "1.20.5",
                                           "1.20.6");
            NMSClassRegistry.registerTyped(NMSReference.class,
                                           "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSReference_spigot_1_20_4",
                                           "1.20.4",
                                           "1.20.4");
        }
    }
}
