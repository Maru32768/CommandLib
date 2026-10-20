package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentTileLocation;
import dev.marulabs.commandlib.internal.nms.world.NMSIBlockData;
import net.minecraft.server.v1_16_R3.ArgumentTileLocation;

public class NMSArgumentTileLocation_spigot_1_16_5 extends NMSArgumentTileLocation {
    public NMSArgumentTileLocation_spigot_1_16_5(Object handle) {
        super(handle, ArgumentTileLocation.class);
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((ArgumentTileLocation) requireHandle()).a());
    }
}
