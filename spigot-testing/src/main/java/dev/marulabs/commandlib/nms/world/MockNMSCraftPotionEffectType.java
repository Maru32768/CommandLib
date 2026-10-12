package dev.marulabs.commandlib.nms.world;

import dev.marulabs.commandlib.nms.argument.MockArgumentTypes;
import dev.marulabs.commandlib.util.nms.world.NMSCraftPotionEffectType;
import dev.marulabs.commandlib.util.nms.world.NMSMobEffectList;
import org.bukkit.potion.PotionEffectType;

public class MockNMSCraftPotionEffectType extends NMSCraftPotionEffectType {
    public MockNMSCraftPotionEffectType() {
        super(null, Object.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        String name = ((MockNMSMobEffectList) nms).getMockName();
        return PotionEffectType.getByName(MockArgumentTypes.bukkitName(name));
    }
}
