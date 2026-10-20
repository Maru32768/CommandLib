package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * The inclusive bounds of a numeric argument or of an integer option. A bound left out is the limit of the type, and a
 * bound the type cannot hold, such as {@code 3e9} for an {@code int}, fails parsing.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Range {
    double min() default -Double.MAX_VALUE;

    double max() default Double.MAX_VALUE;
}
