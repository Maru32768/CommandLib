package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.nms.world.NMSMobEffectList;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentMobEffect extends NMSArgument<NMSMobEffectList> {
    public static NMSArgumentMobEffect create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentMobEffect.class))
                             .newInstance();
    }

    public NMSArgumentMobEffect(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
