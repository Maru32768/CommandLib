package dev.marulabs.commandlib.util;

import dev.marulabs.commandlib.CommonCommandContext;

@FunctionalInterface
public interface UncaughtExceptionHandler<C extends CommonCommandContext<?, ?>> {
    void uncaughtException(C ctx, Throwable e);
}
