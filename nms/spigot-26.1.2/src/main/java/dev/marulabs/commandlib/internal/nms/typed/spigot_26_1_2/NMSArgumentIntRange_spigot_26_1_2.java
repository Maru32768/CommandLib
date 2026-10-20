package dev.marulabs.commandlib.internal.nms.typed.spigot_26_1_2;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentIntRange;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.RangeArgument;

public class NMSArgumentIntRange_spigot_26_1_2 extends NMSArgumentIntRange {
    public NMSArgumentIntRange_spigot_26_1_2() {
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
        Object bounds = context.getArgument(name, Object.class);
        return range((Integer) bound(bounds, "min"), (Integer) bound(bounds, "max"));
    }
}
