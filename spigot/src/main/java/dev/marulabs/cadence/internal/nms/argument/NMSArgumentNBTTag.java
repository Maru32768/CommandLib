package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla NBT compound argument ({@code CompoundTagArgument}), returned as SNBT.
 */
@ApiStatus.Internal
public abstract class NMSArgumentNBTTag extends NMSArgument<String> {
    public static NMSArgumentNBTTag create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentNBTTag.class))
                             .newInstance();
    }

    public NMSArgumentNBTTag(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
