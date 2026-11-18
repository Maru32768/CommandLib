package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentEntity extends NMSArgument<Entity> {
    public static NMSArgumentEntity create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEntity.class))
                             .newInstance();
    }

    public NMSArgumentEntity(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
