package net.kunmc.lab.commandlib.nms.paper_1_21_1;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftItemStack;
import net.kunmc.lab.commandlib.util.nms.world.NMSItemStack;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

public class NMSCraftItemStack_paper_1_21_1 extends NMSCraftItemStack {
    public NMSCraftItemStack_paper_1_21_1() {
        super(null, CraftItemStack.class);
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.getHandle());
    }
}
