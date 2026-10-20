package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentScoreboardSlot;
import net.minecraft.server.v1_16_R3.ArgumentScoreboardSlot;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.Scoreboard;

public class NMSArgumentScoreboardSlot_spigot_1_16_5 extends NMSArgumentScoreboardSlot {
    public NMSArgumentScoreboardSlot_spigot_1_16_5() {
        super(null, ArgumentScoreboardSlot.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentScoreboardSlot.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return Scoreboard.getSlotName(ArgumentScoreboardSlot.a(context, name));
    }
}
