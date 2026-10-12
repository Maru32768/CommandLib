package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.CommonArgument;

public class CommonDoubleArgument<C extends CommonCommandContext<?, ?>, SELF extends CommonDoubleArgument<C, SELF>> extends CommonArgument<Double, C, SELF> {
    public CommonDoubleArgument(String name) {
        super(name, DoubleArgumentType.doubleArg(-Double.MAX_VALUE, Double.MAX_VALUE));
    }

    public CommonDoubleArgument(String name, Double min, Double max) {
        super(name, DoubleArgumentType.doubleArg(min, max));
    }

    @Override
    public final Double cast(Object parsedArgument) {
        return ((Double) parsedArgument);
    }

    @Override
    protected final Double parseImpl(C ctx) {
        return DoubleArgumentType.getDouble(ctx.getHandle(), name());
    }
}
