package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.server.NMSDataPackResources;
import net.kunmc.lab.commandlib.util.nms.server.NMSDedicatedServer;
import net.minecraft.server.v1_16_R3.DedicatedServer;

public class NMSDedicatedServer_spigot_1_16_5 extends NMSDedicatedServer {
    public NMSDedicatedServer_spigot_1_16_5(Object handle) {
        super(handle, "DedicatedServer");
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).getCommandDispatcher());
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSDataPackResources.create(((DedicatedServer) getHandle()).dataPackResources);
    }
}
