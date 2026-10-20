package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.nms.world.NMSEnchantment;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentEnchantment extends NMSArgument<NMSEnchantment> {
    public static NMSArgumentEnchantment create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEnchantment.class))
                             .newInstance();
    }

    public NMSArgumentEnchantment(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
