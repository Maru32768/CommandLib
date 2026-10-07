package net.kunmc.lab.commandlib.nms.spigot_1_19_4;

import net.kunmc.lab.commandlib.util.nms.world.NMSCraftBlockData;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_19_R3.block.data.CraftBlockData;

public class NMSCraftBlockData_spigot_1_19_4 extends NMSCraftBlockData {
    public NMSCraftBlockData_spigot_1_19_4() {
        super(null, "block.data.CraftBlockData");
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.getHandle());
    }
}
