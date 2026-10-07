package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.CommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_paper_1_20_6 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_paper_1_20_6(Object handle) {
        super(handle, "commands.Commands");
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) getHandle()).getDispatcher();
    }
}
