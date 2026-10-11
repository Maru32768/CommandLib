package net.kunmc.lab.commandlib.util.nms.command;

import com.mojang.brigadier.CommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSCommandDispatcher extends NMSClass {
    public static NMSCommandDispatcher create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandDispatcher.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandDispatcher(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract CommandDispatcher<?> getBrigadier();
}
