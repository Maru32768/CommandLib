package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSVec2D extends NMSClass {
    public static NMSVec2D create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVec2D.class), Object.class)
                             .newInstance(handle);
    }

    public NMSVec2D(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract float x();

    public abstract float y();
}
