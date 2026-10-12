package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentItemStack extends NMSArgument<NMSArgumentPredicateItemStack> {
    public static NMSArgumentItemStack create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentItemStack.class))
                             .newInstance();
    }

    public NMSArgumentItemStack(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
