package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.CommandContext;

public class FloatArgument extends CommonFloatArgument<CommandContext, FloatArgument> {
    public FloatArgument(String name) {
        super(name);
    }

    public FloatArgument(String name, float min, float max) {
        super(name, min, max);
    }
}
