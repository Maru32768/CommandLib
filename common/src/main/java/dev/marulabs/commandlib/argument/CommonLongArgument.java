package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.LongArgumentType;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.CommonArgument;

public class CommonLongArgument<C extends CommonCommandContext<?, ?>, SELF extends CommonLongArgument<C, SELF>> extends CommonArgument<Long, C, SELF> {
    public CommonLongArgument(String name) {
        super(name, LongArgumentType.longArg(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    public CommonLongArgument(String name, Long min, Long max) {
        super(name, LongArgumentType.longArg(min, max));
    }

    @Override
    public final Long cast(Object parsedArgument) {
        return ((Long) parsedArgument);
    }

    @Override
    protected final Long parseImpl(C ctx) {
        return LongArgumentType.getLong(ctx.getHandle(), name());
    }
}
