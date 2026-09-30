package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.CommandContext;

import java.util.Collection;
import java.util.function.Supplier;

public class NameableObjectArgument<T extends Nameable> extends CommonNameableObjectArgument<T, CommandContext, NameableObjectArgument<T>> {
    public NameableObjectArgument(String name, Collection<? extends T> candidates) {
        super(name, candidates);
    }

    public NameableObjectArgument(String name, Supplier<Collection<? extends T>> candidatesSupplier) {
        super(name, candidatesSupplier);
    }
}
