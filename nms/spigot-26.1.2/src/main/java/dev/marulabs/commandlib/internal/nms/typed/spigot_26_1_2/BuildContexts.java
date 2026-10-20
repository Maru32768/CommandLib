package dev.marulabs.commandlib.internal.nms.typed.spigot_26_1_2;

import dev.marulabs.commandlib.internal.nms.server.NMSCraftServer;
import net.minecraft.commands.CommandBuildContext;

final class BuildContexts {
    private BuildContexts() {
    }

    /**
     * Returns the build context of the loaded data packs, which argument types backed by registries need.
     */
    static CommandBuildContext current() {
        return (CommandBuildContext) NMSCraftServer.create()
                                                   .getServer()
                                                   .getDataPackResources()
                                                   .getCommandBuildContext()
                                                   .requireHandle();
    }
}
