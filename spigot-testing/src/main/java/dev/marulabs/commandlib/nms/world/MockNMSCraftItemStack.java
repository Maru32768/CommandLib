package dev.marulabs.commandlib.nms.world;

import dev.marulabs.commandlib.util.nms.world.NMSCraftItemStack;
import dev.marulabs.commandlib.util.nms.world.NMSItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MockNMSCraftItemStack extends NMSCraftItemStack {
    public MockNMSCraftItemStack() {
        super(null, Object.class);
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
