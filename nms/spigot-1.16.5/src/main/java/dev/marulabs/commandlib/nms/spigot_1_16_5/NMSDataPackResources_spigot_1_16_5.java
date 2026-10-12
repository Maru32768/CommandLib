package dev.marulabs.commandlib.nms.spigot_1_16_5;

import dev.marulabs.commandlib.util.nms.command.NMSCommandBuildContext;
import dev.marulabs.commandlib.util.nms.server.NMSDataPackResources;
import net.minecraft.server.v1_16_R3.DataPackResources;

public class NMSDataPackResources_spigot_1_16_5 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_16_5(Object handle) {
        super(handle, DataPackResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        throw new UnsupportedOperationException();
    }
}
