package dev.marulabs.cadence.internal.nms.world;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSCraftItemStack extends NMSClass {
    public static NMSCraftItemStack create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftItemStack.class))
                             .newInstance();
    }

    public NMSCraftItemStack(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract ItemStack asCraftMirror(NMSItemStack nms);
}
