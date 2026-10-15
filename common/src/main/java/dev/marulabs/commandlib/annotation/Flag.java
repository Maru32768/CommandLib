package dev.marulabs.commandlib.annotation;

import java.lang.annotation.*;

/**
 * Makes a parameter a flag option: a {@code boolean} parameter gets whether the flag is given, as
 * {@code Options.flag}, and an {@code int} parameter how many times, up to {@link #max()}, as {@code Options.count}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Flag {
    /**
     * The long name, or empty for the parameter name.
     */
    String value() default "";

    /**
     * The short name, or {@code '\0'} for none.
     */
    char shortName() default '\0';

    String[] aliases() default {};

    /**
     * How many times a counted flag can be given; required for an {@code int} parameter and only allowed there.
     */
    int max() default 0;
}
