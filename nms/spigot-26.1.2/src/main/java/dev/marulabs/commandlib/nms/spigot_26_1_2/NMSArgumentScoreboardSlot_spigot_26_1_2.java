package dev.marulabs.commandlib.nms.spigot_26_1_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentScoreboardSlot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;

public class NMSArgumentScoreboardSlot_spigot_26_1_2 extends NMSArgumentScoreboardSlot {
    public NMSArgumentScoreboardSlot_spigot_26_1_2() {
        super(null, ScoreboardSlotArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ScoreboardSlotArgument.displaySlot();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return ScoreboardSlotArgument.getDisplaySlot(context, name).getSerializedName();
    }
}
