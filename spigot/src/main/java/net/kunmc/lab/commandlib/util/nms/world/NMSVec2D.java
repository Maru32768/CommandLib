package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
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
        NMSClassRegistry.register(NMSVec2D.class, NMSVec2D_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSVec2D.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSVec2D_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSVec2D.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSVec2D_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
    }
}
