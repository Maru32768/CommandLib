package dev.marulabs.commandlib.internal.nms.world;

import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
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
