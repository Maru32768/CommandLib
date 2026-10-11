package net.kunmc.lab.commandlib.nms.world;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftBlockData;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class MockNMSCraftBlockData extends NMSCraftBlockData {
    public MockNMSCraftBlockData() {
        super(null, Object.class);
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        String name = ((MockNMSIBlockData) nms).getMaterialName();
        Material material = Material.matchMaterial(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material: " + name);
        }
        return material.createBlockData();
    }
}
