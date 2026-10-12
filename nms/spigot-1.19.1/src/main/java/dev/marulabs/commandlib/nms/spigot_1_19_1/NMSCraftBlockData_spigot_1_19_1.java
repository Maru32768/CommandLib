package dev.marulabs.commandlib.nms.spigot_1_19_1;

import dev.marulabs.commandlib.util.nms.world.NMSCraftBlockData;
import dev.marulabs.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData;

public class NMSCraftBlockData_spigot_1_19_1 extends NMSCraftBlockData {
    public NMSCraftBlockData_spigot_1_19_1() {
        super(null, CraftBlockData.class);
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.getHandle());
    }
}
