package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentItemStack extends NMSArgument<NMSArgumentPredicateItemStack> {
    public static NMSArgumentItemStack create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentItemStack.class))
                             .newInstance();
    }

    public NMSArgumentItemStack(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
