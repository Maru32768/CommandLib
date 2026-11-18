package dev.marulabs.cadence.internal.nms.mock.world;

import dev.marulabs.cadence.internal.nms.mock.argument.MockArgumentTypes;
import dev.marulabs.cadence.internal.nms.world.NMSCraftPotionEffectType;
import dev.marulabs.cadence.internal.nms.world.NMSMobEffectList;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
