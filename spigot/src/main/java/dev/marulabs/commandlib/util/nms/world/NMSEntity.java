package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public abstract class NMSEntity extends NMSClass {
    public static NMSEntity create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSEntity.class), Object.class)
                             .newInstance(handle);
    }

    public NMSEntity(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract Entity getBukkitEntity();
}
