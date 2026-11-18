package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.CommandContext;

public class LongArgument extends CommonLongArgument<CommandContext, LongArgument> {
    public LongArgument(String name) {
        super(name);
    }

    public LongArgument(String name, Long min, Long max) {
        super(name, min, max);
    }
}
