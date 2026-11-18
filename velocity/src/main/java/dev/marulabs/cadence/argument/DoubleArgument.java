package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.CommandContext;

public class DoubleArgument extends CommonDoubleArgument<CommandContext, DoubleArgument> {
    public DoubleArgument(String name) {
        super(name);
    }

    public DoubleArgument(String name, double min, double max) {
        super(name, min, max);
    }
}
