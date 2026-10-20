package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentFloatRange;
import net.minecraft.server.v1_16_R3.ArgumentCriterionValue;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.CriterionConditionValue;

public class NMSArgumentFloatRange_spigot_1_16_5 extends NMSArgumentFloatRange {
    public NMSArgumentFloatRange_spigot_1_16_5() {
        super(null, ArgumentCriterionValue.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return rangeArgument(ArgumentCriterionValue.class, CriterionConditionValue.FloatRange.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Range<Double> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        CriterionConditionValue.FloatRange bounds = context.getArgument(name, CriterionConditionValue.FloatRange.class);
        return doubleRange(bounds.a(), bounds.b());
    }
}
