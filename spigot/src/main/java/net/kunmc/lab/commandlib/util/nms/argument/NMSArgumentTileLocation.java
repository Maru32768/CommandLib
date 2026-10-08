package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentTileLocation_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentTileLocation_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentTileLocation_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentTileLocation extends MinecraftClass {
    public static NMSArgumentTileLocation create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTileLocation.class), Object.class)
                             .newInstance(handle);
    }

    public NMSArgumentTileLocation(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract NMSIBlockData getBlockData();

    static {
        NMSClassRegistry.register(NMSArgumentTileLocation.class,
                                  NMSArgumentTileLocation_v1_16_0.class,
                                  "1.16.0",
                                  "1.16.5");
        NMSClassRegistry.register(NMSArgumentTileLocation.class,
                                  NMSArgumentTileLocation_v1_17_0.class,
                                  "1.17.0",
                                  "1.20.4");
        NMSClassRegistry.register(NMSArgumentTileLocation.class,
                                  NMSArgumentTileLocation_v1_20_5.class,
                                  "1.20.5",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentTileLocation.class);
    }
}
