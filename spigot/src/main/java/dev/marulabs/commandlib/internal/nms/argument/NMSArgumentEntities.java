package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
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
