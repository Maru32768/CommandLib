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
        NMSClassRegistry.registerTyped(NMSCommandDispatcher.class);
    }
}
