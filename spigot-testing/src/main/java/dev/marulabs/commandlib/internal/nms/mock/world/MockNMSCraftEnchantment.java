package dev.marulabs.commandlib.internal.nms.mock.world;

import dev.marulabs.commandlib.internal.nms.mock.argument.MockArgumentTypes;
import dev.marulabs.commandlib.internal.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.internal.nms.world.NMSEnchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSCraftEnchantment extends NMSCraftEnchantment {
    public MockNMSCraftEnchantment() {
        super(null, Object.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        String name = ((MockNMSEnchantment) nms).getMockName();
        return Enchantment.getByKey(NamespacedKey.fromString(MockArgumentTypes.normalizeKey(name)));
    }
}
