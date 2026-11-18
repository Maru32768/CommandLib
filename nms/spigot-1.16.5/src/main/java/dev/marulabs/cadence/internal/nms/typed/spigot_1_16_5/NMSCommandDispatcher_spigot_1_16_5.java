package dev.marulabs.cadence.internal.nms.typed.spigot_1_16_5;

import dev.marulabs.cadence.internal.nms.command.NMSCommandDispatcher;
import net.minecraft.server.v1_16_R3.CommandDispatcher;

public class NMSCommandDispatcher_spigot_1_16_5 extends NMSCommandDispatcher {
    public NMSCommandDispatcher_spigot_1_16_5(Object handle) {
        super(handle, CommandDispatcher.class);
    }

    @Override
    public com.mojang.brigadier.CommandDispatcher<?> getBrigadier() {
        return ((CommandDispatcher) requireHandle()).a();
    }
}
