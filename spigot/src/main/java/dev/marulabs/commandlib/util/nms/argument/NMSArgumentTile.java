package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentTile extends NMSArgument<NMSArgumentTileLocation> {
    public static NMSArgumentTile create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTile.class))
                             .newInstance();
    }

    public NMSArgumentTile(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
