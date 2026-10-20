package dev.marulabs.commandlib.internal.nms.typed.paper_1_21_1;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftBlockData;
import dev.marulabs.commandlib.internal.nms.world.NMSIBlockData;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public class NMSCraftBlockData_paper_1_21_1 extends NMSCraftBlockData {
    public NMSCraftBlockData_paper_1_21_1() {
        super(null, CraftBlockData.class);
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.requireHandle());
    }
}
