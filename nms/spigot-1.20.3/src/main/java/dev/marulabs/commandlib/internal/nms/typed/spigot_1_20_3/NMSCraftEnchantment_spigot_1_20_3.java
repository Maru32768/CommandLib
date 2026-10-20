package dev.marulabs.commandlib.internal.nms.typed.spigot_1_20_3;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.internal.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.v1_20_R3.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_spigot_1_20_3 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_spigot_1_20_3() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CraftEnchantment.minecraftToBukkit((net.minecraft.world.item.enchantment.Enchantment) nms.requireHandle());
    }
}
