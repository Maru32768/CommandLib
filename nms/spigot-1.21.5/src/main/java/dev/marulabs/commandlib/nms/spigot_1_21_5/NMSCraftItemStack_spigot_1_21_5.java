package dev.marulabs.commandlib.nms.spigot_1_21_5;

import dev.marulabs.commandlib.util.nms.world.NMSCraftItemStack;
import dev.marulabs.commandlib.util.nms.world.NMSItemStack;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;

public class NMSCraftItemStack_spigot_1_21_5 extends NMSCraftItemStack {
    public NMSCraftItemStack_spigot_1_21_5() {
        super(null, CraftItemStack.class);
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.getHandle());
    }
}
