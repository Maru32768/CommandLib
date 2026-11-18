package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentDimension extends NMSArgument<World> {
    public static NMSArgumentDimension create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentDimension.class))
                             .newInstance();
    }

    public NMSArgumentDimension(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
