package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.world.NMSItemStack;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

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
