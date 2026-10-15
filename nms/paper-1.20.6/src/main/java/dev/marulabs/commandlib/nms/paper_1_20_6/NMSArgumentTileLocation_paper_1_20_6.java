package dev.marulabs.commandlib.nms.paper_1_20_6;

import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTileLocation;
import dev.marulabs.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.commands.arguments.blocks.BlockInput;

public class NMSArgumentTileLocation_paper_1_20_6 extends NMSArgumentTileLocation {
    public NMSArgumentTileLocation_paper_1_20_6(Object handle) {
        super(handle, BlockInput.class);
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((BlockInput) requireHandle()).getState());
    }
}
