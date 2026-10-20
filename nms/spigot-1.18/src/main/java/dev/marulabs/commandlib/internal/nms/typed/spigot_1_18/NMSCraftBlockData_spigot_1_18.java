package dev.marulabs.commandlib.internal.nms.typed.spigot_1_18;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftBlockData;
import dev.marulabs.commandlib.internal.nms.world.NMSIBlockData;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;

public class NMSCraftBlockData_spigot_1_18 extends NMSCraftBlockData {
    public NMSCraftBlockData_spigot_1_18() {
        super(null, CraftBlockData.class);
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.requireHandle());
    }
}
