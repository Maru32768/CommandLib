package dev.marulabs.cadence.internal.nms.typed.paper_1_20_6;

import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.cadence.internal.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_paper_1_20_6 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_paper_1_20_6(Object handle) {
        super(handle, Commands.class);
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) requireHandle()).getDispatcher();
    }
}
