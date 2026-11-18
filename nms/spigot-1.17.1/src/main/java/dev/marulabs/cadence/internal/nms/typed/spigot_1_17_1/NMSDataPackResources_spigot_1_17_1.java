package dev.marulabs.cadence.internal.nms.typed.spigot_1_17_1;

import dev.marulabs.cadence.internal.nms.command.NMSCommandBuildContext;
import dev.marulabs.cadence.internal.nms.server.NMSDataPackResources;
import net.minecraft.server.ServerResources;

public class NMSDataPackResources_spigot_1_17_1 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_17_1(Object handle) {
        super(handle, ServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        throw new UnsupportedOperationException();
    }
}
