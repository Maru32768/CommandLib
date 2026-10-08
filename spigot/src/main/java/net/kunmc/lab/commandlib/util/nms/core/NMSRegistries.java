package net.kunmc.lab.commandlib.util.nms.core;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.core.v1_19_3.NMSRegistries_v1_19_3;
import net.kunmc.lab.commandlib.util.nms.core.v1_20_0.NMSRegistries_v1_20_0;
import net.kunmc.lab.commandlib.util.nms.core.v1_20_4.NMSRegistries_v1_20_4;
import net.kunmc.lab.commandlib.util.nms.core.v1_20_5.NMSRegistries_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.resources.NMSResourceKey;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

/**
 * for after 1.19.3
 */
public abstract class NMSRegistries extends MinecraftClass {
    public static NMSRegistries create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSRegistries.class))
                             .newInstance();
    }

    public NMSRegistries(Object handle, String className) {
        super(handle, className);
    }

    public abstract NMSResourceKey enchantment();

    public abstract NMSResourceKey mobEffect();

    static {
        NMSClassRegistry.register(NMSRegistries.class, NMSRegistries_v1_19_3.class, "1.19.3", "1.19.4");
        NMSClassRegistry.register(NMSRegistries.class, NMSRegistries_v1_20_0.class, "1.20.0", "1.20.3");
        NMSClassRegistry.register(NMSRegistries.class, NMSRegistries_v1_20_4.class, "1.20.4", "1.20.4");
        NMSClassRegistry.register(NMSRegistries.class, NMSRegistries_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSRegistries.class);
    }
}
