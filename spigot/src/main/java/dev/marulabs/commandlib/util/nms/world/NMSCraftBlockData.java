package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

public abstract class NMSCraftBlockData extends NMSClass {
    public static NMSCraftBlockData create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftBlockData.class))
                             .newInstance();
    }

    public NMSCraftBlockData(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract BlockData createData(NMSIBlockData nms);
}
