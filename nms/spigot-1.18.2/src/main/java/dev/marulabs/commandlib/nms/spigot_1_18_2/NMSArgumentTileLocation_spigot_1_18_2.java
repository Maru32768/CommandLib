package dev.marulabs.commandlib.nms.spigot_1_18_2;

import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTileLocation;
import dev.marulabs.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.commands.arguments.blocks.BlockInput;

public class NMSArgumentTileLocation_spigot_1_18_2 extends NMSArgumentTileLocation {
    public NMSArgumentTileLocation_spigot_1_18_2(Object handle) {
        super(handle, BlockInput.class);
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((BlockInput) requireHandle()).getState());
    }
}
