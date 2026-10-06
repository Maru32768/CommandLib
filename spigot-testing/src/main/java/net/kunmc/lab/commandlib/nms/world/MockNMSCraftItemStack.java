package net.kunmc.lab.commandlib.nms.world;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftItemStack;
import net.kunmc.lab.commandlib.util.nms.world.NMSItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MockNMSCraftItemStack extends NMSCraftItemStack {
    public MockNMSCraftItemStack() {
        super(null, "Mock");
    }

    @Override
    public ItemStack asCraftMirror(NMSItemStack nms) {
        String name = ((MockNMSItemStack) nms).getMaterialName();
        Material material = Material.matchMaterial(name);
        if (material == null) {
            // MockNMSArgumentItemStack rejects unknown items while parsing, so commands never reach this.
            throw new IllegalArgumentException("Unknown item: " + name);
        }
        return new ItemStack(material);
    }
}
