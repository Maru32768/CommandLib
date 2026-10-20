package dev.marulabs.commandlib.internal.nms.world;

import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
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
