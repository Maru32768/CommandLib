package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla NBT compound argument ({@code CompoundTagArgument}), returned as SNBT.
 */
public abstract class NMSArgumentNBTTag extends NMSArgument<String> {
    public static NMSArgumentNBTTag create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentNBTTag.class))
                             .newInstance();
    }

    public NMSArgumentNBTTag(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
