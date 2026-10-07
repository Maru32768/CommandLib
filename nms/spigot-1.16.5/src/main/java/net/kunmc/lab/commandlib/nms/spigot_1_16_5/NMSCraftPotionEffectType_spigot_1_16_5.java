package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftPotionEffectType;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;
import net.minecraft.server.v1_16_R3.MobEffectList;
import org.bukkit.craftbukkit.v1_16_R3.potion.CraftPotionEffectType;
import org.bukkit.potion.PotionEffectType;

public class NMSCraftPotionEffectType_spigot_1_16_5 extends NMSCraftPotionEffectType {
    public NMSCraftPotionEffectType_spigot_1_16_5() {
        this(null);
    }

    public NMSCraftPotionEffectType_spigot_1_16_5(Object handle) {
        super(handle, "potion.CraftPotionEffectType");
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return new CraftPotionEffectType((MobEffectList) nms.getHandle());
    }
}
