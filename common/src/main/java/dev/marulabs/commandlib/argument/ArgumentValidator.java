package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.ArgumentValidationException;

@FunctionalInterface
public interface ArgumentValidator<T, C extends CommonCommandContext<?, ?>> {
    void validate(T value, C ctx) throws ArgumentValidationException;
}
