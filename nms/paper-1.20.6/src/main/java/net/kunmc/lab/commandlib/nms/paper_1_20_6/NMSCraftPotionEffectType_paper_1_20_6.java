package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftPotionEffectType;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;
import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.bukkit.potion.PotionEffectType;

public class NMSCraftPotionEffectType_paper_1_20_6 extends NMSCraftPotionEffectType {
    public NMSCraftPotionEffectType_paper_1_20_6() {
        this(null);
    }

    public NMSCraftPotionEffectType_paper_1_20_6(Object handle) {
        super(handle, "potion.CraftPotionEffectType");
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return CraftPotionEffectType.minecraftToBukkit((MobEffect) nms.getHandle());
    }
}
