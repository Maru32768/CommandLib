package dev.marulabs.commandlib.nms.paper_1_20_6;

import dev.marulabs.commandlib.util.nms.world.NMSCraftBlockData;
import dev.marulabs.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public class NMSCraftBlockData_paper_1_20_6 extends NMSCraftBlockData {
    public NMSCraftBlockData_paper_1_20_6() {
        super(null, CraftBlockData.class);
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.requireHandle());
    }
}
