package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.server.NMSDataPackResources;
import net.kunmc.lab.commandlib.util.nms.server.NMSDedicatedServer;
import net.minecraft.server.dedicated.DedicatedServer;

public class NMSDedicatedServer_paper_1_20_6 extends NMSDedicatedServer {
    public NMSDedicatedServer_paper_1_20_6(Object handle) {
        super(handle, "server.dedicated.DedicatedServer");
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).getCommands());
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSDataPackResources.create(((DedicatedServer) getHandle()).resources.managers());
    }
}
