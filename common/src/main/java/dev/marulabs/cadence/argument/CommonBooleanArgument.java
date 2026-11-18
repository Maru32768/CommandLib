package dev.marulabs.cadence.argument;

import com.mojang.brigadier.arguments.BoolArgumentType;
import dev.marulabs.cadence.CommonArgument;
import dev.marulabs.cadence.CommonCommandContext;

public class CommonBooleanArgument<C extends CommonCommandContext<?, ?>, SELF extends CommonBooleanArgument<C, SELF>> extends CommonArgument<Boolean, C, SELF> {
    public CommonBooleanArgument(String name) {
        super(name, BoolArgumentType.bool());
    }

    @Override
    public final Boolean cast(Object parsedArgument) {
        return ((Boolean) parsedArgument);
    }

    @Override
    protected final Boolean parseImpl(C ctx) {
        return BoolArgumentType.getBool(ctx.getHandle(), name());
    }
}
