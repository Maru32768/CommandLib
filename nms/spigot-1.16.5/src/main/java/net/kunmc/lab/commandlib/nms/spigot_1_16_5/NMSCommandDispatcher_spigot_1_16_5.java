package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.minecraft.server.v1_16_R3.CommandDispatcher;

public class NMSCommandDispatcher_spigot_1_16_5 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_spigot_1_16_5(Object handle) {
        super(handle, "CommandDispatcher");
    }

    @Override
    public com.mojang.brigadier.CommandDispatcher<?> getBrigadier() {
        return ((CommandDispatcher) getHandle()).a();
    }
}
