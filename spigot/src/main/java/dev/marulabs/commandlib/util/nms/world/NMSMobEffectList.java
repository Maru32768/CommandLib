package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSMobEffectList extends NMSClass {
    public static NMSMobEffectList create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSMobEffectList.class), Object.class)
                             .newInstance(handle);
    }

    public NMSMobEffectList(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
