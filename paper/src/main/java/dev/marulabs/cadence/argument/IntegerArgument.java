package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.CommandContext;

public class IntegerArgument extends CommonIntegerArgument<CommandContext, IntegerArgument> {
    public IntegerArgument(String name) {
        super(name);
    }

    public IntegerArgument(String name, int min, int max) {
        super(name, min, max);
    }
}
