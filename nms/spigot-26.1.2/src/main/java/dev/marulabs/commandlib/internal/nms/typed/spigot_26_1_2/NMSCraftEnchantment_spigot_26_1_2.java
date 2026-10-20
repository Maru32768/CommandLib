package dev.marulabs.commandlib.internal.nms.typed.spigot_26_1_2;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.internal.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_spigot_26_1_2 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_spigot_26_1_2() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CraftEnchantment.minecraftToBukkit((net.minecraft.world.item.enchantment.Enchantment) nms.requireHandle());
    }
}
