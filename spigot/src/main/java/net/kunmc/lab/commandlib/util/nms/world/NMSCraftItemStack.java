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
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSCraftItemStack_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSCraftItemStack_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSCraftItemStack_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSCraftItemStack_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSCraftItemStack_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSCraftItemStack_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSCraftItemStack_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSCraftItemStack_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSCraftItemStack.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSCraftItemStack_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
