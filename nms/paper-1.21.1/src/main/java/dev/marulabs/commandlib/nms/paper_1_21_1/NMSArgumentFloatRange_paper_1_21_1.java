package dev.marulabs.commandlib.nms.paper_1_21_1;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentFloatRange;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.RangeArgument;

public class NMSArgumentFloatRange_paper_1_21_1 extends NMSArgumentFloatRange {
    public NMSArgumentFloatRange_paper_1_21_1() {
        super(null, RangeArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return RangeArgument.floatRange();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Range<Double> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        var bounds = RangeArgument.Floats.getRange(context, name);
        return doubleRange(bounds.min().orElse(null), bounds.max().orElse(null));
    }
}
