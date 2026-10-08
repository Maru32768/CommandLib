package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.CraftBukkitClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSCraftItemStack_1_16_0;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.inventory.ItemStack;

public abstract class NMSCraftItemStack extends CraftBukkitClass {
    public static NMSCraftItemStack create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftItemStack.class))
                             .newInstance();
    }

    public NMSCraftItemStack(Object handle, String className) {
        super(handle, className);
    }

    public abstract ItemStack asCraftMirror(NMSItemStack nms);

    static {
        NMSClassRegistry.register(NMSCraftItemStack.class, NMSCraftItemStack_1_16_0.class, "1.16.0", "9.9.9");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class);
    }
}
