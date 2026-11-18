package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.CommandContext;

public class BooleanArgument extends CommonBooleanArgument<CommandContext, BooleanArgument> {
    public BooleanArgument(String name) {
        super(name);
    }
}
