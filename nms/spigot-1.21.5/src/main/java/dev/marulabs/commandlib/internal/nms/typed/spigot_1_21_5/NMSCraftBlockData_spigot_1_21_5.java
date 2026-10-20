package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_5;

import dev.marulabs.commandlib.internal.nms.world.NMSCraftBlockData;
import dev.marulabs.commandlib.internal.nms.world.NMSIBlockData;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R4.block.data.CraftBlockData;

public class NMSCraftBlockData_spigot_1_21_5 extends NMSCraftBlockData {
    public NMSCraftBlockData_spigot_1_21_5() {
        super(null, CraftBlockData.class);
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.requireHandle());
    }
}
