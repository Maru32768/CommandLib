package dev.marulabs.commandlib.nms.spigot_1_21_6;

import dev.marulabs.commandlib.util.nms.command.NMSCommandBuildContext;
import dev.marulabs.commandlib.util.nms.server.NMSDataPackResources;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;

public class NMSDataPackResources_spigot_1_21_6 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_1_21_6(Object handle) {
        super(handle, ReloadableServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        MinecraftServer server = MinecraftServer.getServer();
        return NMSCommandBuildContext.create(CommandBuildContext.simple(server.registryAccess(),
                                                                        server.getWorldData()
                                                                              .enabledFeatures()));
    }
}
