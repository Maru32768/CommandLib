package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.CommandContext;

import java.util.Map;
import java.util.function.Supplier;

public class ObjectArgument<T> extends CommonObjectArgument<T, CommandContext, ObjectArgument<T>> {
    public ObjectArgument(String name, Map<String, ? extends T> nameToObjectMap) {
        super(name, nameToObjectMap);
    }

    public ObjectArgument(String name, Supplier<Map<String, ? extends T>> mapSupplier) {
        super(name, mapSupplier);
    }
}
