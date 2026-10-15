package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Keeps a sender from executing the command again for the duration, as {@code cooldown(...)} does, on a
 * {@link Command} class or a {@link Subcommand} method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Cooldown {
    long value();

    TimeUnit unit() default TimeUnit.SECONDS;
}
