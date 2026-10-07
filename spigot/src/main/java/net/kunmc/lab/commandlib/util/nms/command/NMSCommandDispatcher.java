package net.kunmc.lab.commandlib.util.nms.command;

import com.mojang.brigadier.CommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.command.v1_16_0.NMSCommandDispatcher_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.command.v1_17_0.NMSCommandDispatcher_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.command.v1_20_5.NMSCommandDispatcher_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSCommandDispatcher extends MinecraftClass {
    public static NMSCommandDispatcher create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandDispatcher.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandDispatcher(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract CommandDispatcher<?> getBrigadier();

    static {
        NMSClassRegistry.register(NMSCommandDispatcher.class, NMSCommandDispatcher_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSCommandDispatcher.class, NMSCommandDispatcher_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSCommandDispatcher.class, NMSCommandDispatcher_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSCommandDispatcher_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSCommandDispatcher_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSCommandDispatcher_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSCommandDispatcher_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSCommandDispatcher_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSCommandDispatcher_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSCommandDispatcher_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSCommandDispatcher_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSCommandDispatcher_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
