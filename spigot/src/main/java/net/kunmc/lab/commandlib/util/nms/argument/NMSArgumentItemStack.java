package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentItemStack extends NMSArgument<NMSArgumentPredicateItemStack> {
    public static NMSArgumentItemStack create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentItemStack.class))
                             .newInstance();
    }

    public NMSArgumentItemStack(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
