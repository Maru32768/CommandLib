package dev.marulabs.commandlib.nms.spigot_1_18_2;

import dev.marulabs.commandlib.util.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.v1_18_R2.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_spigot_1_18_2 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_spigot_1_18_2() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return new CraftEnchantment((net.minecraft.world.item.enchantment.Enchantment) nms.requireHandle());
    }
}
