package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * The examples the generated help of the command lists, each a whole input such as {@code /arena create lobby 16},
 * as {@code examples(...)} sets them, on a {@link Command} class or a {@link Subcommand} method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Examples {
    String[] value();
}
