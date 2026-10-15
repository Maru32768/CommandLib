package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandContext;

public class IntegerArgument extends CommonIntegerArgument<CommandContext, IntegerArgument> {
    public IntegerArgument(String name) {
        super(name);
    }

    public IntegerArgument(String name, int min, int max) {
        super(name, min, max);
    }
}
