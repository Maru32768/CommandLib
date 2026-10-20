package dev.marulabs.commandlib.internal.nms.typed.spigot_1_20_4;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentScoreboardSlot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;

public class NMSArgumentScoreboardSlot_spigot_1_20_4 extends NMSArgumentScoreboardSlot {
    public NMSArgumentScoreboardSlot_spigot_1_20_4() {
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
