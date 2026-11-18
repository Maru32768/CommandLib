package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.nms.world.NMSItemStack;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentPredicateItemStack extends NMSClass {
    public static NMSArgumentPredicateItemStack create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentPredicateItemStack.class),
                                             Object.class)
                             .newInstance(handle);
    }

    public NMSArgumentPredicateItemStack(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSItemStack createItemStack(int amount, boolean checkOverStack);
}
