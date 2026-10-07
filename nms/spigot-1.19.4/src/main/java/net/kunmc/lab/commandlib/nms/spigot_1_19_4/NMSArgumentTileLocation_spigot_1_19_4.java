package net.kunmc.lab.commandlib.nms.spigot_1_19_4;

import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTileLocation;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.commands.arguments.blocks.BlockInput;

public class NMSArgumentTileLocation_spigot_1_19_4 extends NMSArgumentTileLocation {
    public NMSArgumentTileLocation_spigot_1_19_4(Object handle) {
        super(handle, "commands.arguments.blocks.ArgumentTileLocation");
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((BlockInput) getHandle()).getState());
    }
}
