package dev.marulabs.cadence.util;

import dev.marulabs.cadence.CommonCommandContext;

@FunctionalInterface
public interface UncaughtExceptionHandler<C extends CommonCommandContext<?, ?>> {
    void uncaughtException(C ctx, Throwable e);
}
