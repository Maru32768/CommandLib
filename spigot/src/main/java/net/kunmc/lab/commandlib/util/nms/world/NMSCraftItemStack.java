package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.inventory.ItemStack;

public abstract class NMSCraftItemStack extends NMSClass {
    public static NMSCraftItemStack create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftItemStack.class))
                             .newInstance();
    }

    public NMSCraftItemStack(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract ItemStack asCraftMirror(NMSItemStack nms);
}
