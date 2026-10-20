package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_4;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftPotionEffectType;
import dev.marulabs.commandlib.internal.nms.world.NMSMobEffectList;
import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.v1_21_R3.potion.CraftPotionEffectType;
import org.bukkit.potion.PotionEffectType;

public class NMSCraftPotionEffectType_spigot_1_21_4 extends NMSCraftPotionEffectType {
    public NMSCraftPotionEffectType_spigot_1_21_4() {
        super(null, CraftPotionEffectType.class);
    }

    public NMSCraftPotionEffectType_spigot_1_21_4(Object handle) {
        super(handle, CraftPotionEffectType.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return CraftPotionEffectType.minecraftToBukkit((MobEffect) nms.requireHandle());
    }
}
