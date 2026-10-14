package dev.marulabs.commandlib.nms.spigot_1_21_3;

import dev.marulabs.commandlib.util.nms.server.NMSCraftServer;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_21_R2.CraftServer;

public class NMSCraftServer_spigot_1_21_3 extends NMSCraftServer {
    public NMSCraftServer_spigot_1_21_3(Server handle) {
        super(handle, CraftServer.class);
    }

    @Override
    public NMSDedicatedServer getServer() {
        return NMSDedicatedServer.create(((CraftServer) getHandle()).getServer());
    }

    @Override
    public SimpleCommandMap getCommandMap() {
        return ((CraftServer) getHandle()).getCommandMap();
    }
}
