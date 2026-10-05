package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.CommandContext;

public class DoubleArgument extends CommonDoubleArgument<CommandContext, DoubleArgument> {
    public DoubleArgument(String name) {
        super(name);
    }

    public DoubleArgument(String name, Double min, Double max) {
        super(name, min, max);
    }
}
