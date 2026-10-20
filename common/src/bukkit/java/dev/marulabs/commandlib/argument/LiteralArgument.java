package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandContext;

import java.util.Collection;
import java.util.function.Supplier;

public class LiteralArgument extends CommonLiteralArgument<CommandContext, LiteralArgument> {
    public LiteralArgument(String name, Collection<String> literals) {
        super(name, literals);
    }

    public LiteralArgument(String name, Supplier<Collection<String>> literalsSupplier) {
        super(name, literalsSupplier);
    }
}
