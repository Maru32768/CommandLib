package dev.marulabs.commandlib.internal.nms.typed.spigot_1_18;

import dev.marulabs.commandlib.internal.nms.command.NMSCommandBuildContext;
import dev.marulabs.commandlib.internal.nms.server.NMSDataPackResources;
import net.minecraft.server.ServerResources;

public class NMSDataPackResources_spigot_1_18 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_18(Object handle) {
        super(handle, ServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        throw new UnsupportedOperationException();
    }
}
