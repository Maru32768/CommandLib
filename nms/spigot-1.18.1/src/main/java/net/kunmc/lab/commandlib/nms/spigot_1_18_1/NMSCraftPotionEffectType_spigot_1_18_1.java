package net.kunmc.lab.commandlib.nms.spigot_1_18_1;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftPotionEffectType;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;
import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.v1_18_R1.potion.CraftPotionEffectType;
import org.bukkit.potion.PotionEffectType;

public class NMSCraftPotionEffectType_spigot_1_18_1 extends NMSCraftPotionEffectType {
    public NMSCraftPotionEffectType_spigot_1_18_1() {
        this(null);
    }

    public NMSCraftPotionEffectType_spigot_1_18_1(Object handle) {
        super(handle, "potion.CraftPotionEffectType");
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return new CraftPotionEffectType((MobEffect) nms.getHandle());
    }
}
