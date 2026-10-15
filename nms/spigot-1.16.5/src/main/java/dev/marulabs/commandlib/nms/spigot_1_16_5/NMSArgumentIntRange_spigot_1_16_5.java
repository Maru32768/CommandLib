package dev.marulabs.commandlib.nms.spigot_1_16_5;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentIntRange;
import net.minecraft.server.v1_16_R3.ArgumentCriterionValue;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import net.minecraft.server.v1_16_R3.CriterionConditionValue;

public class NMSArgumentIntRange_spigot_1_16_5 extends NMSArgumentIntRange {
    public NMSArgumentIntRange_spigot_1_16_5() {
        super(null, ArgumentCriterionValue.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentCriterionValue.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Range<Integer> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        CriterionConditionValue.IntegerRange bounds = ArgumentCriterionValue.b.a(context, name);
        return range(bounds.a(), bounds.b());
    }
}
