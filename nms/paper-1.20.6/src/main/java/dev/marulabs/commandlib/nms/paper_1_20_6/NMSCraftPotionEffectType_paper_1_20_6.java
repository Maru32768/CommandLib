package dev.marulabs.commandlib.nms.paper_1_20_6;

import dev.marulabs.commandlib.util.nms.world.NMSCraftPotionEffectType;
import dev.marulabs.commandlib.util.nms.world.NMSMobEffectList;
import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.bukkit.potion.PotionEffectType;

public class NMSCraftPotionEffectType_paper_1_20_6 extends NMSCraftPotionEffectType {
    public NMSCraftPotionEffectType_paper_1_20_6() {
        super(null, CraftPotionEffectType.class);
    }

    public NMSCraftPotionEffectType_paper_1_20_6(Object handle) {
        super(handle, CraftPotionEffectType.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return CraftPotionEffectType.minecraftToBukkit((MobEffect) nms.requireHandle());
    }
}
