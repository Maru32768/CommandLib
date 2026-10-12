package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandContext;

public class LongArgument extends CommonLongArgument<CommandContext, LongArgument> {
    public LongArgument(String name) {
        super(name);
    }

    public LongArgument(String name, Long min, Long max) {
        super(name, min, max);
    }
}
