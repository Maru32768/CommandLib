package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTime;

public class MockNMSArgumentTime extends NMSArgumentTime {
    public MockNMSArgumentTime() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return argument(0);
    }

    @Override
    public ArgumentType<?> argument(int minimum) {
        return MockArgumentTypes.time(minimum);
    }

    @Override
    protected Integer parseImpl(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Integer.class);
    }
}
