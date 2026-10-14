package dev.marulabs.commandlib.nms.spigot_1_21_11;

import dev.marulabs.commandlib.util.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.v1_21_R7.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_spigot_1_21_11 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_spigot_1_21_11() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CraftEnchantment.minecraftToBukkit((net.minecraft.world.item.enchantment.Enchantment) nms.getHandle());
    }
}
