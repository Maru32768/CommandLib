package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandContext;

public class BooleanArgument extends CommonBooleanArgument<CommandContext, BooleanArgument> {
    public BooleanArgument(String name) {
        super(name);
    }
}
