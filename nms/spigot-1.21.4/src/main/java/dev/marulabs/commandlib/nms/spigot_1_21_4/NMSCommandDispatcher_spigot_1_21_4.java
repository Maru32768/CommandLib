package dev.marulabs.commandlib.nms.spigot_1_21_4;

import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_spigot_1_21_4 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_spigot_1_21_4(Object handle) {
        super(handle, Commands.class);
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) getHandle()).getDispatcher();
    }
}
