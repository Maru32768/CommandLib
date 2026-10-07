package net.kunmc.lab.commandlib.nms.spigot_1_20_4;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftEnchantment;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.v1_20_R3.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_spigot_1_20_4 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_spigot_1_20_4() {
        super(null, "enchantments.CraftEnchantment");
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CraftEnchantment.minecraftToBukkit((net.minecraft.world.item.enchantment.Enchantment) nms.getHandle());
    }
}
