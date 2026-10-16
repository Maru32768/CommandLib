package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Sets how many values a {@code List} argument parameter accepts, as {@code variadic(max)} does; without it the
 * argument accepts as many as {@code variadic()}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Variadic {
    int max();
}
