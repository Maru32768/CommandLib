package net.kunmc.lab.commandlib.nms.spigot_1_17_1;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandBuildContext;
import net.kunmc.lab.commandlib.util.nms.server.NMSDataPackResources;
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
