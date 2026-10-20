package dev.marulabs.commandlib.internal.nms.typed.spigot_1_20_2;

import com.mojang.brigadier.CommandDispatcher;
import dev.marulabs.commandlib.internal.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_spigot_1_20_2 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_spigot_1_20_2(Object handle) {
        super(handle, Commands.class);
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) requireHandle()).getDispatcher();
    }
}
