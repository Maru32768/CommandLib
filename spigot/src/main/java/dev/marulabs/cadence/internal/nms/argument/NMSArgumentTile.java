package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentTile extends NMSArgument<NMSArgumentTileLocation> {
    public static NMSArgumentTile create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTile.class))
                             .newInstance();
    }

    public NMSArgumentTile(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
