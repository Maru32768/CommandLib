package dev.marulabs.commandlib.nms.spigot_1_17_1;

import dev.marulabs.commandlib.util.nms.server.NMSCraftServer;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;

public class NMSCraftServer_spigot_1_17_1 extends NMSCraftServer {
    public NMSCraftServer_spigot_1_17_1(Server handle) {
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
