package dev.marulabs.commandlib.internal.nms.mock.world;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftItemStack;
import dev.marulabs.commandlib.internal.nms.world.NMSItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
