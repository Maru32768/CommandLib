package dev.marulabs.cadence.internal.nms.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
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
