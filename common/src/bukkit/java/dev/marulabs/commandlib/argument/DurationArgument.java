package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandContext;

public class DurationArgument extends CommonDurationArgument<CommandContext, DurationArgument> {
    public DurationArgument(String name) {
        super(name);
    }
}
