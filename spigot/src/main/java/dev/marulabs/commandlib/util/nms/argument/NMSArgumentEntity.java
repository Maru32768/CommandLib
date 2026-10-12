package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;

public abstract class NMSArgumentEntity extends NMSArgument<Entity> {
    public static NMSArgumentEntity create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEntity.class))
                             .newInstance();
    }

    public NMSArgumentEntity(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
