package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.block.data.BlockData;

public abstract class NMSCraftBlockData extends NMSClass {
    public static NMSCraftBlockData create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftBlockData.class))
                             .newInstance();
    }

    public NMSCraftBlockData(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract BlockData createData(NMSIBlockData nms);
}
