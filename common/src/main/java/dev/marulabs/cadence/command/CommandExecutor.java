package dev.marulabs.cadence.command;

import dev.marulabs.cadence.CommonCommandContext;
import dev.marulabs.cadence.exception.CommandPrerequisiteException;

@FunctionalInterface
public interface CommandExecutor<C extends CommonCommandContext<?, ?>> {
    void accept(C ctx) throws CommandPrerequisiteException;
}
