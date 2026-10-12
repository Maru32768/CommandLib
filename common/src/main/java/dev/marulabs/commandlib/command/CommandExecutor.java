package dev.marulabs.commandlib.command;

import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

@FunctionalInterface
public interface CommandExecutor<C extends CommonCommandContext<?, ?>> {
    void accept(C ctx) throws CommandPrerequisiteException;
}
