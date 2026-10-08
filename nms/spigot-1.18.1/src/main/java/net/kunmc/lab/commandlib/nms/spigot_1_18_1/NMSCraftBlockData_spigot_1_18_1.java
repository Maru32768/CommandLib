package net.kunmc.lab.commandlib.nms.spigot_1_18_1;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftBlockData;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;

public class NMSCraftBlockData_spigot_1_18_1 extends NMSCraftBlockData {
    public NMSCraftBlockData_spigot_1_18_1() {
        super(null, "block.data.CraftBlockData");
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.getHandle());
    }
}
