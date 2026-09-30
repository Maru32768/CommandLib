package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.CommandContext;

public class FloatArgument extends CommonFloatArgument<CommandContext, FloatArgument> {
    public FloatArgument(String name) {
        super(name);
    }

    public FloatArgument(String name, Float min, Float max) {
        super(name, min, max);
    }
}
