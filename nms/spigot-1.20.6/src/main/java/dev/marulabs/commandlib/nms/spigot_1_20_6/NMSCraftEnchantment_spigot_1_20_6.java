package dev.marulabs.commandlib.nms.spigot_1_20_6;

import dev.marulabs.commandlib.util.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.v1_20_R4.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_spigot_1_20_6 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_spigot_1_20_6() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CraftEnchantment.minecraftToBukkit((net.minecraft.world.item.enchantment.Enchantment) nms.requireHandle());
    }
}
