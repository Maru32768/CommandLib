package dev.marulabs.commandlib.nms.argument;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentIntRange;

public class MockNMSArgumentIntRange extends NMSArgumentIntRange {
    public MockNMSArgumentIntRange() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockArgumentTypes.bounds(Integer::valueOf);
    }

    @Override
    protected Range<Integer> parseImpl(CommandContext<?> ctx, String name) {
        Object[] bounds = ctx.getArgument(name, Object[].class);
        return range((Integer) bounds[0], (Integer) bounds[1]);
    }
}
