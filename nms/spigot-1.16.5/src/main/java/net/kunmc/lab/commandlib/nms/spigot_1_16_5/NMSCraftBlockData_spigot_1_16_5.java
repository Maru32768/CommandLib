package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftBlockData;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.server.v1_16_R3.IBlockData;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;

public class NMSCraftBlockData_spigot_1_16_5 extends NMSCraftBlockData {
    public NMSCraftBlockData_spigot_1_16_5() {
        super(null, "block.data.CraftBlockData");
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((IBlockData) nms.getHandle());
    }
}
