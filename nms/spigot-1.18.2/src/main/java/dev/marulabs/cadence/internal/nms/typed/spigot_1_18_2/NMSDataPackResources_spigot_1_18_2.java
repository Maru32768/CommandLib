package dev.marulabs.cadence.internal.nms.typed.spigot_1_18_2;

import dev.marulabs.cadence.internal.nms.command.NMSCommandBuildContext;
import dev.marulabs.cadence.internal.nms.server.NMSDataPackResources;
import net.minecraft.server.ReloadableServerResources;

public class NMSDataPackResources_spigot_1_18_2 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_18_2(Object handle) {
        super(handle, ReloadableServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        throw new UnsupportedOperationException();
    }
}
