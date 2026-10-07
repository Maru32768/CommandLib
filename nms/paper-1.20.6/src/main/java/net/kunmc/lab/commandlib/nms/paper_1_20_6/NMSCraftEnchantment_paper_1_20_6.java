package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftEnchantment;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_paper_1_20_6 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_paper_1_20_6() {
        super(null, "enchantments.CraftEnchantment");
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CraftEnchantment.minecraftToBukkit((net.minecraft.world.item.enchantment.Enchantment) nms.getHandle());
    }
}
