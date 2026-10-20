package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_3;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentScoreboardSlot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;

public class NMSArgumentScoreboardSlot_spigot_1_21_3 extends NMSArgumentScoreboardSlot {
    public NMSArgumentScoreboardSlot_spigot_1_21_3() {
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
