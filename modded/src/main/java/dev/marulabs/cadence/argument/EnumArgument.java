package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.CommandContext;

public class EnumArgument<T extends Enum<T>> extends CommonEnumArgument<T, CommandContext, EnumArgument<T>> {
    public EnumArgument(String name, Class<T> clazz) {
        super(name, clazz);
    }
}
