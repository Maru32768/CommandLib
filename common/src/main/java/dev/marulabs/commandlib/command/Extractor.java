package dev.marulabs.commandlib.command;

import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

@FunctionalInterface
public interface Extractor<C extends CommonCommandContext<?, ?>, S> {
    S extract(C ctx) throws CommandPrerequisiteException;
}
