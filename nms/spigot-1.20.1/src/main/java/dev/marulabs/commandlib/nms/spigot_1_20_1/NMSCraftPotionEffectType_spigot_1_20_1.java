package dev.marulabs.commandlib.nms.spigot_1_20_1;

import dev.marulabs.commandlib.util.nms.world.NMSCraftPotionEffectType;
import dev.marulabs.commandlib.util.nms.world.NMSMobEffectList;
import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.v1_20_R1.potion.CraftPotionEffectType;
import org.bukkit.potion.PotionEffectType;

public class NMSCraftPotionEffectType_spigot_1_20_1 extends NMSCraftPotionEffectType {
    public NMSCraftPotionEffectType_spigot_1_20_1() {
        this(null);
    }

    public NMSCraftPotionEffectType_spigot_1_20_1(Object handle) {
        super(handle, CraftPotionEffectType.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return new CraftPotionEffectType((MobEffect) nms.getHandle());
    }
}
