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
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSArgumentEntities_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSArgumentEntities_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSArgumentEntities_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSArgumentEntities_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSArgumentEntities_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSArgumentEntities_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSArgumentEntities_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSArgumentEntities_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSArgumentEntities.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSArgumentEntities_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
