package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentItemStack_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentItemStack_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_19_0.NMSArgumentItemStack_v1_19_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentItemStack_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentItemStack extends NMSArgument<NMSArgumentPredicateItemStack> {
    public static NMSArgumentItemStack create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentItemStack.class))
                             .newInstance();
    }

    public NMSArgumentItemStack(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentItemStack.class, NMSArgumentItemStack_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentItemStack.class, NMSArgumentItemStack_v1_17_0.class, "1.17.0", "1.18.2");
        NMSClassRegistry.register(NMSArgumentItemStack.class, NMSArgumentItemStack_v1_19_0.class, "1.19.0", "1.20.4");
        NMSClassRegistry.register(NMSArgumentItemStack.class, NMSArgumentItemStack_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class);
    }
}
