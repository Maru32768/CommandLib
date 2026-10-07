package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.CraftBukkitClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSCraftBlockData_v1_16_0;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.block.data.BlockData;

public abstract class NMSCraftBlockData extends CraftBukkitClass {
    public static NMSCraftBlockData create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftBlockData.class))
                             .newInstance();
    }

    public NMSCraftBlockData(Object handle, String className) {
        super(handle, className);
    }

    public abstract BlockData createData(NMSIBlockData nms);

    static {
        NMSClassRegistry.register(NMSCraftBlockData.class, NMSCraftBlockData_v1_16_0.class, "1.16.0", "9.9.9");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSCraftBlockData_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSCraftBlockData_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSCraftBlockData_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSCraftBlockData_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSCraftBlockData_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSCraftBlockData_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSCraftBlockData_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSCraftBlockData_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSCraftBlockData_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
