package dev.marulabs.commandlib.internal.nms.typed.spigot_26_1_2;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentFloatRange;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.RangeArgument;

public class NMSArgumentFloatRange_spigot_26_1_2 extends NMSArgumentFloatRange {
    public NMSArgumentFloatRange_spigot_26_1_2() {
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
        Object bounds = context.getArgument(name, Object.class);
        return doubleRange((Number) bound(bounds, "min"), (Number) bound(bounds, "max"));
    }
}
