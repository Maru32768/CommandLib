package net.kunmc.lab.commandlib.nms.spigot_1_20_4;

import com.mojang.brigadier.CommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.minecraft.commands.Commands;

public class NMSCommandDispatcher_spigot_1_20_4 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_spigot_1_20_4(Object handle) {
        super(handle, "commands.CommandDispatcher");
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) getHandle()).getDispatcher();
    }
}
