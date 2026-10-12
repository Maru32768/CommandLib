package dev.marulabs.commandlib.util.nms.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSCommandDispatcher extends NMSClass {
    public static NMSCommandDispatcher create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandDispatcher.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandDispatcher(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract CommandDispatcher<?> getBrigadier();
}
