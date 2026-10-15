package dev.marulabs.commandlib.nms.spigot_1_21_11;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentIntRange;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.RangeArgument;

public class NMSArgumentIntRange_spigot_1_21_11 extends NMSArgumentIntRange {
    public NMSArgumentIntRange_spigot_1_21_11() {
        super(null, RangeArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return RangeArgument.intRange();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Range<Integer> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        var bounds = RangeArgument.Ints.getRange(context, name);
        return range(bounds.min().orElse(null), bounds.max().orElse(null));
    }
}
