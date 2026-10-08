package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentEntities_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentEntities_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_18_0.NMSArgumentEntities_v1_18_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentEntities_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;

import java.util.List;

public abstract class NMSArgumentEntities extends NMSArgument<List<Entity>> {
    public static NMSArgumentEntities create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEntities.class))
                             .newInstance();
    }

    public NMSArgumentEntities(Object handle, String className) {
        super(handle, className);
    }

    static {
        NMSClassRegistry.register(NMSArgumentEntities.class, NMSArgumentEntities_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentEntities.class, NMSArgumentEntities_v1_17_0.class, "1.17.0", "1.17.1");
        NMSClassRegistry.register(NMSArgumentEntities.class, NMSArgumentEntities_v1_18_0.class, "1.18.0", "1.20.4");
        NMSClassRegistry.register(NMSArgumentEntities.class, NMSArgumentEntities_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class);
    }
}
