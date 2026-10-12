package dev.marulabs.commandlib.nms.spigot_1_18_1;

import dev.marulabs.commandlib.util.nms.command.NMSCommandBuildContext;
import dev.marulabs.commandlib.util.nms.server.NMSDataPackResources;
import net.minecraft.server.ServerResources;

public class NMSDataPackResources_spigot_1_18_1 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_18_1(Object handle) {
        super(handle, ServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        throw new UnsupportedOperationException();
    }
}
