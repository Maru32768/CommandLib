package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;

import java.util.List;

public abstract class NMSArgumentEntities extends NMSArgument<List<Entity>> {
    public static NMSArgumentEntities create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEntities.class))
                             .newInstance();
    }

    public NMSArgumentEntities(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
