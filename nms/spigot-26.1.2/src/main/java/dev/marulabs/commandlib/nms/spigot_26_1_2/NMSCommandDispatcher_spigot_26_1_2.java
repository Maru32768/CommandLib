package dev.marulabs.commandlib.nms.spigot_26_1_2;

import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_spigot_26_1_2 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_spigot_26_1_2(Object handle) {
        super(handle, Commands.class);
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) getHandle()).getDispatcher();
    }
}
