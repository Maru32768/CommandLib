package dev.marulabs.commandlib.nms.spigot_1_19_1;

import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_spigot_1_19_1 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_spigot_1_19_1(Object handle) {
        super(handle, Commands.class);
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) requireHandle()).getDispatcher();
    }
}
