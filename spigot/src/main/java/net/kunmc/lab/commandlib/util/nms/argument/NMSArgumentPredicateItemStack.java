package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.NMSItemStack;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentPredicateItemStack extends NMSClass {
    public static NMSArgumentPredicateItemStack create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentPredicateItemStack.class),
                                             Object.class)
                             .newInstance(handle);
    }

    public NMSArgumentPredicateItemStack(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSItemStack createItemStack(int amount, boolean checkOverStack);
}
