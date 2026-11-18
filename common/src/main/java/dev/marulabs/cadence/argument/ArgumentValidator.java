package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.CommonCommandContext;
import dev.marulabs.cadence.exception.ArgumentValidationException;

@FunctionalInterface
public interface ArgumentValidator<T, C extends CommonCommandContext<?, ?>> {
    void validate(C ctx, T value) throws ArgumentValidationException;
}
