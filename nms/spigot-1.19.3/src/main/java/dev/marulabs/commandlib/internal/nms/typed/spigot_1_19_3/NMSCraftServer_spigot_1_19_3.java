package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19_3;

import dev.marulabs.commandlib.internal.nms.server.NMSCraftServer;
import dev.marulabs.commandlib.internal.nms.server.NMSDedicatedServer;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_19_R2.CraftServer;

public class NMSCraftServer_spigot_1_19_3 extends NMSCraftServer {
    public NMSCraftServer_spigot_1_19_3(Server handle) {
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
