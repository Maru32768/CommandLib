package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19_3;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftItemStack;
import dev.marulabs.commandlib.internal.nms.world.NMSItemStack;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;

public class NMSCraftItemStack_spigot_1_19_3 extends NMSCraftItemStack {
    public NMSCraftItemStack_spigot_1_19_3() {
        super(null, CraftItemStack.class);
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.requireHandle());
    }
}
