package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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
