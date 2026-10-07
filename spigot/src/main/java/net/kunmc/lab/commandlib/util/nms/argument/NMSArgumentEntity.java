package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentEntity_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentEntity_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentEntity_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;

public abstract class NMSArgumentEntity extends NMSArgument<Entity> {
    public static NMSArgumentEntity create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEntity.class))
                             .newInstance();
    }

    public NMSArgumentEntity(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentEntity.class, NMSArgumentEntity_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentEntity.class, NMSArgumentEntity_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSArgumentEntity.class, NMSArgumentEntity_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSArgumentEntity_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSArgumentEntity_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSArgumentEntity_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSArgumentEntity_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSArgumentEntity_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSArgumentEntity_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSArgumentEntity_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSArgumentEntity_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSArgumentEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSArgumentEntity_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
