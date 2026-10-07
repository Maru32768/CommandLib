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
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSArgumentItemStack_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSArgumentItemStack_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSArgumentItemStack_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSArgumentItemStack_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSArgumentItemStack_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSArgumentItemStack_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSArgumentItemStack_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSArgumentItemStack_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSArgumentItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSArgumentItemStack_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
