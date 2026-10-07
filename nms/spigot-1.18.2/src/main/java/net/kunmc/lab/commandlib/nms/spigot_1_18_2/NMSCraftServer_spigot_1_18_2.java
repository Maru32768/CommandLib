package net.kunmc.lab.commandlib.nms.spigot_1_18_2;

import net.kunmc.lab.commandlib.util.nms.server.NMSCraftServer;
import net.kunmc.lab.commandlib.util.nms.server.NMSDedicatedServer;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;

public class NMSCraftServer_spigot_1_18_2 extends NMSCraftServer {
    public NMSCraftServer_spigot_1_18_2(Server handle) {
        super(handle, "CraftServer");
    }

    @Override
    public NMSDedicatedServer getServer() {
        return NMSDedicatedServer.create(((CraftServer) getHandle()).getServer());
    }
}
