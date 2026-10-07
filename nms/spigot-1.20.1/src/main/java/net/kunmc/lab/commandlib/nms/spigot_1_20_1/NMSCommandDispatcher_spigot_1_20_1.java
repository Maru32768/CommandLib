package net.kunmc.lab.commandlib.nms.spigot_1_20_1;

import com.mojang.brigadier.CommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_spigot_1_20_1 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_spigot_1_20_1(Object handle) {
        super(handle, "commands.CommandDispatcher");
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) getHandle()).getDispatcher();
    }
}
