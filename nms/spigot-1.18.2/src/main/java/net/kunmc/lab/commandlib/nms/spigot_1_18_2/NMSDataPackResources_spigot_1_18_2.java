package net.kunmc.lab.commandlib.nms.spigot_1_18_2;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandBuildContext;
import net.kunmc.lab.commandlib.util.nms.server.NMSDataPackResources;
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
