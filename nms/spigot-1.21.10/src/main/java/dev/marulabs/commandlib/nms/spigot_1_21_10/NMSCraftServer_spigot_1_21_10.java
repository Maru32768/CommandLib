package dev.marulabs.commandlib.nms.spigot_1_21_10;

import dev.marulabs.commandlib.util.nms.server.NMSCraftServer;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_21_R6.CraftServer;

public class NMSCraftServer_spigot_1_21_10 extends NMSCraftServer {
    public NMSCraftServer_spigot_1_21_10(Server handle) {
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
