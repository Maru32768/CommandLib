package dev.marulabs.commandlib.command;

import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

@FunctionalInterface
public interface Prerequisite<C extends CommonCommandContext<?, ?>> {
    void check(C ctx) throws CommandPrerequisiteException;

    default Prerequisite<C> and(Prerequisite<C> other) {
        return ctx -> {
            check(ctx);
            other.check(ctx);
        };
    }
}
