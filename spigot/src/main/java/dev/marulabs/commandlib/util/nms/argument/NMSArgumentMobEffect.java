package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.world.NMSMobEffectList;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentMobEffect extends NMSArgument<NMSMobEffectList> {
    public static NMSArgumentMobEffect create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentMobEffect.class))
                             .newInstance();
    }

    public NMSArgumentMobEffect(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
