package net.kunmc.lab.commandlib.nms.spigot_1_20_6;

import net.kunmc.lab.commandlib.util.nms.server.NMSCraftServer;
import net.kunmc.lab.commandlib.util.nms.server.NMSDedicatedServer;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;

public class NMSCraftServer_spigot_1_20_6 extends NMSCraftServer {
    public NMSCraftServer_spigot_1_20_6(Server handle) {
        super(handle, "CraftServer");
    }

    @Override
    public NMSDedicatedServer getServer() {
        return NMSDedicatedServer.create(((CraftServer) getHandle()).getServer());
    }
}
