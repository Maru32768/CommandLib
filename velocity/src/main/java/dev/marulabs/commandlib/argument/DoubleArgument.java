package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandContext;

public class DoubleArgument extends CommonDoubleArgument<CommandContext, DoubleArgument> {
    public DoubleArgument(String name) {
        super(name);
    }

    public DoubleArgument(String name, double min, double max) {
        super(name, min, max);
    }
}
