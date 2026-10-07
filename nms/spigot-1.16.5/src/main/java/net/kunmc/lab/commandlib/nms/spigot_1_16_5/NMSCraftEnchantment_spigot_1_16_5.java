package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftEnchantment;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.craftbukkit.v1_16_R3.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;

public class NMSCraftEnchantment_spigot_1_16_5 extends NMSCraftEnchantment {
    public NMSCraftEnchantment_spigot_1_16_5() {
        super(null, "enchantments.CraftEnchantment");
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return new CraftEnchantment((net.minecraft.server.v1_16_R3.Enchantment) nms.getHandle());
    }
}
