package dev.marulabs.cadence.command;

import dev.marulabs.cadence.CommonCommandContext;
import dev.marulabs.cadence.exception.CommandPrerequisiteException;

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
