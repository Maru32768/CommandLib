package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSVec2D_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_17_0.NMSVec2D_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_20_5.NMSVec2D_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSVec2D extends MinecraftClass {
    public static NMSVec2D create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVec2D.class), Object.class)
                             .newInstance(handle);
    }

    public NMSVec2D(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract float x();

    public abstract float y();

    static {
        // Spigot keeps the obfuscated field names i (x) and j (y) up to 1.20.4.
        NMSClassRegistry.register(NMSVec2D.class, NMSVec2D_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSVec2D.class, NMSVec2D_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSVec2D.class, NMSVec2D_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSVec2D.class);
    }
}
