package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSMobEffectList extends NMSClass {
    public static NMSMobEffectList create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSMobEffectList.class), Object.class)
                             .newInstance(handle);
    }

    public NMSMobEffectList(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
