package net.kunmc.lab.commandlib.nms.spigot_1_20_6;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandBuildContext;
import net.kunmc.lab.commandlib.util.nms.server.NMSDataPackResources;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.server.MinecraftServer;

public class NMSDataPackResources_spigot_1_20_6 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_20_6(Object handle) {
        super(handle, "server.DataPackResources");
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        MinecraftServer server = MinecraftServer.getServer();
        return NMSCommandBuildContext.create(CommandBuildContext.simple(server.registryAccess(),
                                                                        server.getWorldData()
                                                                              .enabledFeatures()));
    }
}
