package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;

public class CommonIntegerArgument<C extends CommonCommandContext<?, ?>, SELF extends CommonIntegerArgument<C, SELF>> extends CommonArgument<Integer, C, SELF> {
    public CommonIntegerArgument(String name) {
        super(name, IntegerArgumentType.integer(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    public CommonIntegerArgument(String name, Integer min, Integer max) {
        super(name, IntegerArgumentType.integer(min, max));
    }

    @Override
    public final Integer cast(Object parsedArgument) {
        return ((Integer) parsedArgument);
    }

    @Override
    protected final Integer parseImpl(C ctx) {
        return IntegerArgumentType.getInteger(ctx.getHandle(), name());
    }
}
