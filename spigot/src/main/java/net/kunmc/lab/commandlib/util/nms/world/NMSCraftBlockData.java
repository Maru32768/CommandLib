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
        NMSClassRegistry.registerTyped(NMSCraftBlockData.class);
    }
}
