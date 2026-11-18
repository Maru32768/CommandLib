package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.CommandContext;

public class DurationArgument extends CommonDurationArgument<CommandContext, DurationArgument> {
    public DurationArgument(String name) {
        super(name);
    }
}
