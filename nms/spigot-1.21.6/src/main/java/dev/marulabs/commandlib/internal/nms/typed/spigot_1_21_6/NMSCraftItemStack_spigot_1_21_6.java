package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_6;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftItemStack;
import dev.marulabs.commandlib.internal.nms.world.NMSItemStack;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;

public class NMSCraftItemStack_spigot_1_21_6 extends NMSCraftItemStack {
    public NMSCraftItemStack_spigot_1_21_6() {
        super(null, CraftItemStack.class);
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.requireHandle());
    }
}
