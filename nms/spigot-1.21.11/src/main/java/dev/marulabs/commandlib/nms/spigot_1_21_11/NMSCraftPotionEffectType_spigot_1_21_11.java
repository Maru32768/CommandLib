package dev.marulabs.commandlib.nms.spigot_1_21_11;

import dev.marulabs.commandlib.util.nms.world.NMSCraftPotionEffectType;
import dev.marulabs.commandlib.util.nms.world.NMSMobEffectList;
import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.v1_21_R7.potion.CraftPotionEffectType;
import org.bukkit.potion.PotionEffectType;

public class NMSCraftPotionEffectType_spigot_1_21_11 extends NMSCraftPotionEffectType {
    public NMSCraftPotionEffectType_spigot_1_21_11() {
        this(null);
    }

    public NMSCraftPotionEffectType_spigot_1_21_11(Object handle) {
        super(handle, CraftPotionEffectType.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return CraftPotionEffectType.minecraftToBukkit((MobEffect) nms.getHandle());
    }
}
