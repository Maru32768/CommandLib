package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.internal.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.v1_19_R1.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_spigot_1_19 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_spigot_1_19() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return new CraftEnchantment((net.minecraft.world.item.enchantment.Enchantment) nms.requireHandle());
    }
}
