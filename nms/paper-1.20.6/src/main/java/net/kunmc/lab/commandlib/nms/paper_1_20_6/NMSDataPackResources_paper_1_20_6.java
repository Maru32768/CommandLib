package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import io.papermc.paper.command.brigadier.PaperCommands;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandBuildContext;
import net.kunmc.lab.commandlib.util.nms.server.NMSDataPackResources;

public class NMSDataPackResources_paper_1_20_6 extends NMSDataPackResources {
    public NMSDataPackResources_paper_1_20_6(Object handle) {
        super(handle, "server.ReloadableServerResources");
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        return NMSCommandBuildContext.create(PaperCommands.INSTANCE.getBuildContext());
    }
}
