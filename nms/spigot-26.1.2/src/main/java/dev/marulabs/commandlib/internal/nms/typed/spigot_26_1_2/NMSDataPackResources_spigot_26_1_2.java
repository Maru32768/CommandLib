package dev.marulabs.commandlib.internal.nms.typed.spigot_26_1_2;

import dev.marulabs.commandlib.internal.nms.command.NMSCommandBuildContext;
import dev.marulabs.commandlib.internal.nms.server.NMSDataPackResources;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;

public class NMSDataPackResources_spigot_26_1_2 extends NMSDataPackResources {
    public NMSDataPackResources_spigot_26_1_2(Object handle) {
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
