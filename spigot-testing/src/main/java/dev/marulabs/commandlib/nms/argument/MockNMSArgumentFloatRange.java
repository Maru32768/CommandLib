package dev.marulabs.commandlib.nms.argument;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentFloatRange;

public class MockNMSArgumentFloatRange extends NMSArgumentFloatRange {
    public MockNMSArgumentFloatRange() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockArgumentTypes.bounds(Double::valueOf);
    }

    @Override
    protected Range<Double> parseImpl(CommandContext<?> ctx, String name) {
        Object[] bounds = ctx.getArgument(name, Object[].class);
        return doubleRange((Double) bounds[0], (Double) bounds[1]);
    }
}
