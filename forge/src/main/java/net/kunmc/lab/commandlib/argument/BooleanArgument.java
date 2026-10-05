package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.CommandContext;

public class BooleanArgument extends CommonBooleanArgument<CommandContext, BooleanArgument> {
    public BooleanArgument(String name) {
        super(name);
    }
}
