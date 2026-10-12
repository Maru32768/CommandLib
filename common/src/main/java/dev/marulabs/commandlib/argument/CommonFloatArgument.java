package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.FloatArgumentType;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;

public class CommonFloatArgument<C extends CommonCommandContext<?, ?>, SELF extends CommonFloatArgument<C, SELF>> extends CommonArgument<Float, C, SELF> {
    public CommonFloatArgument(String name) {
        super(name, FloatArgumentType.floatArg(-Float.MAX_VALUE, Float.MAX_VALUE));
    }

    public CommonFloatArgument(String name, Float min, Float max) {
        super(name, FloatArgumentType.floatArg(min, max));
    }

    @Override
    public final Float cast(Object parsedArgument) {
        return ((Float) parsedArgument);
    }

    @Override
    protected final Float parseImpl(C ctx) {
        return FloatArgumentType.getFloat(ctx.getHandle(), name());
    }
}
