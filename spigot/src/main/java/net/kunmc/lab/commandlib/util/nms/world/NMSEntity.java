package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSEntity_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_17_0.NMSEntity_v1_17_0;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;

public abstract class NMSEntity extends MinecraftClass {
    public static NMSEntity create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSEntity.class), Object.class)
                             .newInstance(handle);
    }

    public NMSEntity(Object handle, String className) {
        super(handle, className);
    }

    public abstract Entity getBukkitEntity();

    static {
        NMSClassRegistry.register(NMSEntity.class, NMSEntity_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSEntity.class, NMSEntity_v1_17_0.class, "1.17.0", "9.9.9");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSEntity_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSEntity_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSEntity_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSEntity_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSEntity_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSEntity_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSEntity_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSEntity_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSEntity.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSEntity_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
