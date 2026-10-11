package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentMobEffect extends NMSArgument<NMSMobEffectList> {
    public static NMSArgumentMobEffect create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentMobEffect.class))
                             .newInstance();
    }

    public NMSArgumentMobEffect(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
