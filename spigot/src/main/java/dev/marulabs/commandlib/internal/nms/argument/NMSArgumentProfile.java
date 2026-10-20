package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentProfile extends NMSArgument<Object> {
    public static NMSArgumentProfile create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentProfile.class))
                             .newInstance();
    }

    public NMSArgumentProfile(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
