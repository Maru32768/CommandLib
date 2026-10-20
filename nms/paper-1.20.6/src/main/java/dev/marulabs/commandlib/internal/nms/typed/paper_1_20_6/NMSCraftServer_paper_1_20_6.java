package dev.marulabs.commandlib.internal.nms.typed.paper_1_20_6;

import dev.marulabs.commandlib.internal.nms.server.NMSCraftServer;
import dev.marulabs.commandlib.internal.nms.server.NMSDedicatedServer;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.CraftServer;

public class NMSCraftServer_paper_1_20_6 extends NMSCraftServer {
    public NMSCraftServer_paper_1_20_6(Server handle) {
        super(handle, CraftServer.class);
    }

    @Override
    public NMSDedicatedServer getServer() {
        return NMSDedicatedServer.create(((CraftServer) requireHandle()).getServer());
    }

    @Override
    public SimpleCommandMap getCommandMap() {
        return ((CraftServer) requireHandle()).getCommandMap();
    }
}
