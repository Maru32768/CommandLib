package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTileLocation;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;
import net.minecraft.server.v1_16_R3.ArgumentTileLocation;

public class NMSArgumentTileLocation_spigot_1_16_5 extends NMSArgumentTileLocation {
    public NMSArgumentTileLocation_spigot_1_16_5(Object handle) {
        super(handle, "ArgumentTileLocation");
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((ArgumentTileLocation) getHandle()).a());
    }
}
