package dev.marulabs.commandlib.nms.world;

import dev.marulabs.commandlib.nms.argument.MockArgumentTypes;
import dev.marulabs.commandlib.util.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

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
