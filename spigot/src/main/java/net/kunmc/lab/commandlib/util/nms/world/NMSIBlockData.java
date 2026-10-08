package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSIBlockData_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_17_0.NMSIBlockData_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_20_5.NMSIBlockData_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public class NMSIBlockData extends MinecraftClass {
    public static NMSIBlockData create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSIBlockData.class), Object.class)
                             .newInstance(handle);
    }

    public NMSIBlockData(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSIBlockData.class, NMSIBlockData_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSIBlockData.class, NMSIBlockData_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSIBlockData.class, NMSIBlockData_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSIBlockData.class);
    }
}
