package net.kunmc.lab.commandlib.nms.world;

import net.kunmc.lab.commandlib.nms.argument.MockArgumentTypes;
import net.kunmc.lab.commandlib.util.nms.world.NMSCraftEnchantment;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public class MockNMSCraftEnchantment extends NMSCraftEnchantment {
    public MockNMSCraftEnchantment() {
        super(null, "Mock");
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        String name = ((MockNMSEnchantment) nms).getMockName();
        return Enchantment.getByKey(NamespacedKey.fromString(MockArgumentTypes.normalizeKey(name)));
    }
}
