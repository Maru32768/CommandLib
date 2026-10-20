package dev.marulabs.commandlib.internal.nms.typed.paper_1_21_1;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftPotionEffectType;
import dev.marulabs.commandlib.internal.nms.world.NMSMobEffectList;
import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.bukkit.potion.PotionEffectType;

public class NMSCraftPotionEffectType_paper_1_21_1 extends NMSCraftPotionEffectType {
    public NMSCraftPotionEffectType_paper_1_21_1() {
        super(null, CraftPotionEffectType.class);
    }

    public NMSCraftPotionEffectType_paper_1_21_1(Object handle) {
        super(handle, CraftPotionEffectType.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return CraftPotionEffectType.minecraftToBukkit((MobEffect) nms.requireHandle());
    }
}
