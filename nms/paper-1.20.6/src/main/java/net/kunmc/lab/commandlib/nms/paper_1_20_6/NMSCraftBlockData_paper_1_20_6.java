package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftBlockData;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public class NMSCraftBlockData_paper_1_20_6 extends NMSCraftBlockData {
    public NMSCraftBlockData_paper_1_20_6() {
        super(null, "block.data.CraftBlockData");
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.getHandle());
    }
}
