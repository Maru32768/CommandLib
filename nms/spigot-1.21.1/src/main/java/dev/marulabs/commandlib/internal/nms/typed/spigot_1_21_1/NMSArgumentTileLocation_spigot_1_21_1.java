package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_1;

import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentTileLocation;
import dev.marulabs.commandlib.internal.nms.world.NMSIBlockData;
import net.minecraft.commands.arguments.blocks.BlockInput;

public class NMSArgumentTileLocation_spigot_1_21_1 extends NMSArgumentTileLocation {
    public NMSArgumentTileLocation_spigot_1_21_1(Object handle) {
        super(handle, BlockInput.class);
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((BlockInput) requireHandle()).getState());
    }
}
