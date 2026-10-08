package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentNamespacedKey_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentNamespacedKey_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentNamespacedKey_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentNamespacedKey extends NMSArgument<String> {
    public static NMSArgumentNamespacedKey create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentNamespacedKey.class))
                             .newInstance();
    }

    public NMSArgumentNamespacedKey(Object handle, String className) {
        super(handle, className);
    }

    static {
        NMSClassRegistry.register(NMSArgumentNamespacedKey.class,
                                  NMSArgumentNamespacedKey_v1_16_0.class,
                                  "1.16.0",
                                  "1.16.5");
        NMSClassRegistry.register(NMSArgumentNamespacedKey.class,
                                  NMSArgumentNamespacedKey_v1_17_0.class,
                                  "1.17.0",
                                  "1.20.4");
        NMSClassRegistry.register(NMSArgumentNamespacedKey.class,
                                  NMSArgumentNamespacedKey_v1_20_5.class,
                                  "1.20.5",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentNamespacedKey.class);
    }
}
