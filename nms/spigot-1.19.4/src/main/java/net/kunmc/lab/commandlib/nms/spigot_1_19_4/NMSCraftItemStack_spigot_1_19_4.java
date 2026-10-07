package net.kunmc.lab.commandlib.nms.spigot_1_19_4;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftItemStack;
import net.kunmc.lab.commandlib.util.nms.world.NMSItemStack;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;

public class NMSCraftItemStack_spigot_1_19_4 extends NMSCraftItemStack {
    public NMSCraftItemStack_spigot_1_19_4() {
        super(null, "inventory.CraftItemStack");
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.getHandle());
    }
}
