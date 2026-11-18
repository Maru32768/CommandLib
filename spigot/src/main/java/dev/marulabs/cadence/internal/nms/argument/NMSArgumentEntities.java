package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@ApiStatus.Internal
public abstract class NMSArgumentEntities extends NMSArgument<List<Entity>> {
    public static NMSArgumentEntities create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEntities.class))
                             .newInstance();
    }

    public NMSArgumentEntities(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
