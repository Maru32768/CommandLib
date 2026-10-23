package dev.marulabs.commandlib.command;

import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

/**
 * Reads a value from the context, or fails the command by throwing {@link CommandPrerequisiteException}. It is also the
 * {@link Prerequisite} that checks the same without the value, so {@code require(Command.player())} gives the executor
 * the player and {@code addPrerequisite(Command.player())} only lets players run the command.
 */
@FunctionalInterface
public interface Extractor<C extends CommonCommandContext<?, ?>, S> extends Prerequisite<C> {
    S extract(C ctx) throws CommandPrerequisiteException;

    @Override
    default void check(C ctx) throws CommandPrerequisiteException {
        extract(ctx);
    }
}
