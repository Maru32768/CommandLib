package net.kunmc.lab.commandlib.nms.paper_1_21_1;

import com.mojang.brigadier.CommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_paper_1_21_1 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_paper_1_21_1(Object handle) {
        super(handle, Commands.class);
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) getHandle()).getDispatcher();
    }
}
