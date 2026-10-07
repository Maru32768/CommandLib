package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTileLocation;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.commands.arguments.blocks.BlockInput;

public class NMSArgumentTileLocation_paper_1_20_6 extends NMSArgumentTileLocation {
    public NMSArgumentTileLocation_paper_1_20_6(Object handle) {
        super(handle, "commands.arguments.blocks.BlockInput");
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((BlockInput) getHandle()).getState());
    }
}
