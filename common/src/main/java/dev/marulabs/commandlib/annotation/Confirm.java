package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Asks the sender to run the command again within {@link #seconds()} before it executes, as {@code confirm(...)}
 * does, on a {@link Command} class or a {@link Subcommand} method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Confirm {
    long seconds() default 10;
}
