package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandContext;

public class EnumArgument<T extends Enum<T>> extends CommonEnumArgument<T, CommandContext, EnumArgument<T>> {
    public EnumArgument(String name, Class<T> clazz) {
        super(name, clazz);
    }
}
